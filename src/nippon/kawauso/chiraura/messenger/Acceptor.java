/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.connection.InetAddressFunctions;
import nippon.kawauso.chiraura.lib.connection.TrafficLimiter;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 受け入れた接続を準備する。
 * @author chirauraNoSakusha
 */
final class Acceptor implements Callable<Void> {

    private static final Logger LOG = Logger.getLogger(Acceptor.class.getName());

    // 参照。
    // 接続制限。
    private final boolean portIgnore;
    private final int connectionLimit;

    // 入出力。
    private final BlockingQueue<MessengerReport> messengerReportSink;
    private final ConnectionPool<AcceptedConnection> acceptedConnectionPool;

    // 通信周り。
    private final int sendBufferSize;
    private final long connectionTimeout;
    private final long operationTimeout;
    private final Transceiver.Share transceiverShare;

    private final AcceptedConnection acceptedConnection;

    // プロトコル周り。
    private final long version;
    private final long versionGapThreshold;
    private final KeyPair id;
    private final PublicKeyManager keyManager;
    private final AtomicReference<InetSocketAddress> self;

    // 主に後続のために。
    private final ExecutorService executor;
    private final SendQueuePool sendQueuePool;
    private final BlockingQueue<ReceivedMail> receivedMailSink;
    private final TrafficLimiter limiter;
    private final ConnectionPool<Connection> connectionPool;
    private final long keyLifetime;

    Acceptor(final boolean portIgnore, final int connectionLimit, final BlockingQueue<MessengerReport> messengerReportSink,
            final ConnectionPool<AcceptedConnection> acceptedConnectionPool, final int sendBufferSize, final long connectionTimeout,
            final long operationTimeout, final Transceiver.Share transceiverShare, final AcceptedConnection acceptedConnection, final long version,
            final long versionGapThreshold, final KeyPair id, final PublicKeyManager keyManager, final AtomicReference<InetSocketAddress> self,
            final ExecutorService executor, final SendQueuePool sendQueuePool, final BlockingQueue<ReceivedMail> receivedMailSink,
            final TrafficLimiter limiter, final ConnectionPool<Connection> connectionPool, final long keyLifetime) {
        if (connectionLimit < 0) {
            throw new IllegalArgumentException("Negative connection limit ( " + connectionLimit + " ).");
        } else if (messengerReportSink == null) {
            throw new IllegalArgumentException("Null messenger report sink.");
        } else if (acceptedConnectionPool == null) {
            throw new IllegalArgumentException("Null accepted connection pool.");
        } else if (connectionTimeout < 0) {
            throw new IllegalArgumentException("Invalid connection timeout ( " + connectionTimeout + " ).");
        } else if (operationTimeout < 0) {
            throw new IllegalArgumentException("Invalid operation timeout ( " + operationTimeout + " ).");
        } else if (acceptedConnection == null) {
            throw new IllegalArgumentException("Null connection.");
        } else if (transceiverShare == null) {
            throw new IllegalArgumentException("Null transceiver share.");
        } else if (versionGapThreshold < 1) {
            throw new IllegalArgumentException("Invalid version gap threshold ( " + versionGapThreshold + " ).");
        } else if (id == null) {
            throw new IllegalArgumentException("Null id.");
        } else if (keyManager == null) {
            throw new IllegalArgumentException("Null key manager.");
        } else if (self == null) {
            throw new IllegalArgumentException("Null self.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        } else if (sendQueuePool == null) {
            throw new IllegalArgumentException("Null send queue pool.");
        } else if (receivedMailSink == null) {
            throw new IllegalArgumentException("Null received mail sink.");
        } else if (limiter == null) {
            throw new IllegalArgumentException("Null limiter.");
        } else if (connectionPool == null) {
            throw new IllegalArgumentException("Null connection pool.");
        } else if (keyLifetime < 0) {
            throw new IllegalArgumentException("Invalid key lifetime ( " + keyLifetime + " ).");
        }

        this.portIgnore = portIgnore;
        this.connectionLimit = connectionLimit;

        this.messengerReportSink = messengerReportSink;
        this.acceptedConnectionPool = acceptedConnectionPool;

        this.sendBufferSize = sendBufferSize;
        this.connectionTimeout = connectionTimeout;
        this.operationTimeout = operationTimeout;
        this.acceptedConnection = acceptedConnection;
        this.transceiverShare = transceiverShare;

        this.version = version;
        this.versionGapThreshold = versionGapThreshold;
        this.id = id;
        this.keyManager = keyManager;
        this.self = self;

        this.executor = executor;
        this.receivedMailSink = receivedMailSink;
        this.limiter = limiter;
        this.sendQueuePool = sendQueuePool;
        this.connectionPool = connectionPool;
        this.keyLifetime = keyLifetime;
    }

    @Override
    public Void call() {
        LOG.log(Level.FINE, "{0}: こんにちは。", this.acceptedConnection);

        try {
            subCall();
        } catch (final Exception e) {
            if (!Thread.currentThread().isInterrupted() && !this.acceptedConnection.isClosed()) {
                // 別プロセスが接続を閉じて終了を報せてくれたわけでもない。
                // 通信異常はさして珍しいものではない。
                LOG.log(Level.FINER, (new StringBuilder()).append(this.acceptedConnection).append(": 異常が発生しました").toString(), e);
                this.acceptedConnection.close();
                ConcurrentFunctions.completePut(new AcceptanceError(this.acceptedConnection.getDestination(), e), this.messengerReportSink);
            }
        } finally {
            // 登録の削除。
            this.acceptedConnectionPool.remove(this.acceptedConnection.getIdNumber());
        }

        LOG.log(Level.FINE, "{0}: さようなら。", this.acceptedConnection);
        return null;
    }

    private boolean isOverConnectionLimit(final int numOfConnections) {
        if (numOfConnections < this.connectionLimit) {
            return false;
        }

        LOG.log(Level.WARNING, "{0}: 接続数 ( {1} ) が限界 ( {2} ) に達しています。", new Object[] { this.acceptedConnection, numOfConnections, this.connectionLimit });
        this.acceptedConnection.close();
        return true;
    }

    private boolean isOverConnectionLimit(final InetSocketAddress destination) {
        // acceptedConnectionPool を含めてはいけない。
        return isOverConnectionLimit(this.connectionPool.getNumOfConnections(destination));
    }

    private boolean isOverPortIgnoringConnectionLimit(final InetSocketAddress destination) {
        return isOverConnectionLimit(this.acceptedConnectionPool.getNumOfConnections(destination) + this.connectionPool.getNumOfConnections(destination));
    }

    private boolean isOverPortIgnoringConnectionLimit() {
        return isOverPortIgnoringConnectionLimit(this.acceptedConnection.getDestination());
    }

    private void subCall() throws IOException, MyRuleException {

        // ポートを気にしないなら、ここで接続数制限。
        if (this.portIgnore && isOverPortIgnoringConnectionLimit()) {
            this.acceptedConnection.close();
            return;
        }

        // 受信の時間制限を設定。
        this.acceptedConnection.getSocket().setSoTimeout((int) this.operationTimeout);

        // 送信バッファサイズを設定。
        // 受信バッファサイズは Server で設定してある。
        final int oldSendBufferSize = this.acceptedConnection.getSocket().getSendBufferSize();
        if (this.acceptedConnection.getSocket().getSendBufferSize() < this.sendBufferSize) {
            this.acceptedConnection.getSocket().setSendBufferSize(this.sendBufferSize);
            LOG.log(Level.FINEST, "{0}: 送信バッファサイズを {1} から {2} に変更しました。", new Object[] { this.acceptedConnection, oldSendBufferSize, this.sendBufferSize });
        }

        final InputStream input = new BufferedInputStream(this.acceptedConnection.getSocket().getInputStream(),
                this.acceptedConnection.getSocket().getReceiveBufferSize());
        final OutputStream output = new BufferedOutputStream(this.acceptedConnection.getSocket().getOutputStream(),
                this.acceptedConnection.getSocket().getSendBufferSize());

        final Transceiver transceiver = new Transceiver(this.transceiverShare, input, output, false);

        // 一言目を受信。
        final Message message1 = StartingProtocol.receiveFirst(transceiver);

        if (message1 instanceof FirstMessage) {
            acceptSequence((FirstMessage) message1, transceiver);
        } else if (message1 instanceof PortCheckMessage) {
            portCheckReaction((PortCheckMessage) message1, transceiver);
        } else {
            throw new MyRuleException("Invalid first message.");
        }
    }

    private void acceptSequence(final FirstMessage message, final Transceiver transceiver) throws IOException, MyRuleException {
        final PublicKey destinationPublicKey = message.getKey();

        // 一言目への相槌を送信。
        final Key communicationKey = CryptographicKeys.newCommonKey();
        StartingProtocol.sendFirstReply(transceiver, destinationPublicKey, communicationKey);

        // 二言目を受信。
        final SecondMessage message2 = StartingProtocol.receiveSecond(transceiver, communicationKey);
        final PublicKey destinationId = message2.getId();
        final byte[] signedCommunicationKeyBytes = message2.getEncryptedKey();
        final byte[] watchword = message2.getWatchword();
        final long destinationVersion = message2.getVersion();
        final int destinationPort = message2.getPort();
        final int connectionType = message2.getType();
        final InetSocketAddress declaredSelf = message2.getPeer();

        // 相手が識別用鍵の所持者かどうか検査。
        if (!Arrays.equals(communicationKey.getEncoded(), CryptographicFunctions.decrypt(destinationId, signedCommunicationKeyBytes))) {
            throw new MyRuleException("Invalid destination id.");
        }

        final InetSocketAddress destination = new InetSocketAddress(this.acceptedConnection.getSocket().getInetAddress(), destinationPort);

        if (destinationVersion != this.version) {
            // 版が合わない。

            if (this.version < destinationVersion) {
                ConcurrentFunctions.completePut(new NewProtocolWarning(this.version, destinationVersion), this.messengerReportSink);
            } else {
                LOG.log(Level.FINEST, "{0}: 自分 ( 第 {1} 版 ) より古い個体 ( 第 {2} 版 ) を検知しました。",
                        new Object[] { this.acceptedConnection, Long.toString(this.version), Long.toString(destinationVersion) });
            }

            if (Math.abs(this.version - destinationVersion) >= this.versionGapThreshold) {
                // 離れすぎ。

                // さよなら (二言目への相槌) を送信。
                final KeyPair keyPair = this.keyManager.getPublicKeyPair();
                StartingProtocol
                        .sendSecondReply(transceiver, communicationKey, this.id, watchword, keyPair.getPublic(), this.version, destination);

                this.acceptedConnection.close();
                return;
            }
        }

        // ポート検査。
        if (portCheck(destination, destinationId)) {

            // ポートを気にする接続数制限。
            if (!this.portIgnore && isOverConnectionLimit(destination)) {
                this.acceptedConnection.close();
                return;
            }

            // 二言目への相槌を送信。
            final KeyPair keyPair = this.keyManager.getPublicKeyPair();
            StartingProtocol.sendSecondReply(transceiver, communicationKey, this.id, watchword, keyPair.getPublic(), this.version, destination);

            // 準備が終わったので報告。
            updateSelf(declaredSelf, this.acceptedConnection.getSocket().getInetAddress());
            ConcurrentFunctions.completePut(new ConnectReport(destinationId, destination, connectionType), this.messengerReportSink);

            // 無通信での接続保持期間を設定。
            this.acceptedConnection.getSocket().setSoTimeout((int) this.connectionTimeout);

            // 本格的な送受信の開始。
            final Connection connection = new Connection(this.acceptedConnection.getIdNumber(), destination, destinationId, connectionType,
                    this.acceptedConnection.getSocket());
            this.connectionPool.add(connection);
            LOG.log(Level.FINER, "{0}: {1} と種別 {2} で通信を開始します。", new Object[] { this.acceptedConnection, destination, Integer.toString(connectionType) });
            connection.setSender(this.executor.submit(new Sender(this.sendQueuePool, this.messengerReportSink, this.connectionPool, this.connectionTimeout,
                    transceiver, connection, this.keyLifetime, keyPair.getPrivate(), destinationPublicKey, communicationKey)));
            this.executor.submit(new Receiver(this.receivedMailSink, this.messengerReportSink, this.limiter, this.connectionTimeout, transceiver, connection,
                    keyPair.getPrivate(), destinationPublicKey, communicationKey));
        } else {
            // ポート異常を通知。
            StartingProtocol.sendPortError(transceiver, communicationKey);

            this.acceptedConnection.close();
        }
    }

    private boolean portCheck(final InetSocketAddress destination, final PublicKey destinationId) throws IOException, MyRuleException {
        try (final Socket socket = new Socket()) {
            socket.setSoTimeout((int) this.operationTimeout);

            try {
                socket.connect(destination, (int) this.operationTimeout);
            } catch (final IOException e) {
                if (e instanceof SocketTimeoutException) {
                    LOG.log(Level.FINEST, "{0}: {1} への接続は時間切れしました。", new Object[] { this.acceptedConnection, destination });
                } else if (e instanceof ConnectException) {
                    LOG.log(Level.FINEST, "{0}: {1} への接続が拒否されました。", new Object[] { this.acceptedConnection, destination });
                } else {
                    LOG.log(Level.FINER, "異常が発生しました", e);
                    LOG.log(Level.FINEST, "{0}: {1} への接続が失敗しました", new Object[] { this.acceptedConnection, destination });
                }
                return false;
            }
            LOG.log(Level.FINEST, "{0}: ポート検査用に {1} に接続しました。", new Object[] { this.acceptedConnection, destination });

            final InputStream input = new BufferedInputStream(socket.getInputStream());
            final OutputStream output = new BufferedOutputStream(socket.getOutputStream());
            final Transceiver transceiver = new Transceiver(this.transceiverShare, input, output, true);

            // 検査用の言付けを送信。
            final Key communicationKey = CryptographicKeys.newCommonKey();
            StartingProtocol.sendPortCheck(transceiver, destinationId, communicationKey);

            // 検査用の言付けへの相槌を受信。
            final PortCheckReply reply = StartingProtocol.receivePortCheckReply(transceiver, communicationKey);
            if (reply == null) {
                return false;
            }

            return reply.getValue() == Arrays.hashCode(communicationKey.getEncoded());
        }
    }

    private void portCheckReaction(final PortCheckMessage message, final Transceiver transceiver) throws IOException {
        // 検査用の言付けへの相槌を送信。
        final Key communicationKey = CryptographicKeys.getCommonKey(CryptographicFunctions.decrypt(this.id.getPrivate(), message.getEncryptedKey()));
        StartingProtocol.sendPortCheckReply(transceiver, communicationKey);

        this.acceptedConnection.close();
    }

    private void updateSelf(final InetSocketAddress declaredSelf, final InetAddress destination) {
        // 外聞の更新。
        final InetSocketAddress oldSelf = this.self.get();
        final InetSocketAddress newSelf = InetAddressFunctions.selectBetter(oldSelf, declaredSelf);
        if (!newSelf.equals(oldSelf)) {
            this.self.set(newSelf);
            ConcurrentFunctions.completePut(new SelfReport(newSelf, destination), this.messengerReportSink);
        }
    }

}
