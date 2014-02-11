/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.connection.InetAddressFunctions;
import nippon.kawauso.chiraura.lib.connection.PortFunctions;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 他の個体へ接続して通信開始交渉を行う。
 * @author chirauraNoSakusha
 */
final class Contactor implements Callable<Void> {

    private static final Logger LOG = Logger.getLogger(Acceptor.class.getName());

    // 参照。
    // 入出力。
    private final BlockingQueue<MessengerReport> messengerReportSink;
    private final BoundConnectionPool<ContactingConnection> contactingConnectionPool;

    // 通信周り。
    private final int receiveBufferSize;
    private final int sendBufferSize;
    private final long connectionTimeout;
    private final long operationTimeout;
    private final Transceiver transceiver;

    private final ContactingConnection contactingConnection;

    // プロトコル周り。
    private final long version;
    private final long versionGapThreshold;
    private final int port;
    private final KeyPair id;
    private final PublicKeyManager keyManager;
    private final AtomicReference<InetSocketAddress> self;

    // 主に後続のために。
    private final ExecutorService executor;
    private final SendQueuePool sendQueuePool;
    private final BlockingQueue<ReceivedMail> receivedMailSink;
    private final BoundConnectionPool<Connection> connectionPool;
    private final long keyLifetime;

    Contactor(final BlockingQueue<MessengerReport> messengerReportSink, final BoundConnectionPool<ContactingConnection> contactingConnectionPool,
            final int receiveBufferSize, final int sendBufferSize, final long connectionTimeout, final long operationTimeout, final Transceiver transceiver,
            final ContactingConnection contactingConnection, final long version, final long versionGapThreshold, final int port, final KeyPair id,
            final PublicKeyManager keyManager, final AtomicReference<InetSocketAddress> self, final ExecutorService executor,
            final SendQueuePool sendQueuePool, final BlockingQueue<ReceivedMail> receivedMailSink, final BoundConnectionPool<Connection> connectionPool,
            final long keyLifetime) {
        if (messengerReportSink == null) {
            throw new IllegalArgumentException("Null messenger report sink.");
        } else if (contactingConnectionPool == null) {
            throw new IllegalArgumentException("Null contacting connection pool.");
        } else if (connectionTimeout < 0) {
            throw new IllegalArgumentException("Negative connection timeout ( " + connectionTimeout + " ).");
        } else if (operationTimeout < 0) {
            throw new IllegalArgumentException("Invalid operation timeout ( " + operationTimeout + " ).");
        } else if (transceiver == null) {
            throw new IllegalArgumentException("Null transceiver.");
        } else if (contactingConnection == null) {
            throw new IllegalArgumentException("Null connection.");
        } else if (versionGapThreshold < 1) {
            throw new IllegalArgumentException("Invalid version gap threshold ( " + versionGapThreshold + " ).");
        } else if (!PortFunctions.isValid(port)) {
            throw new IllegalArgumentException("Invalid port ( " + port + " ).");
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
        } else if (connectionPool == null) {
            throw new IllegalArgumentException("Null connection pool.");
        } else if (keyLifetime < 0) {
            throw new IllegalArgumentException("Invalid key lifetime ( " + keyLifetime + " ).");
        }

        this.messengerReportSink = messengerReportSink;
        this.contactingConnectionPool = contactingConnectionPool;

        this.receiveBufferSize = receiveBufferSize;
        this.sendBufferSize = sendBufferSize;
        this.connectionTimeout = connectionTimeout;
        this.operationTimeout = operationTimeout;
        this.contactingConnection = contactingConnection;
        this.transceiver = transceiver;

        this.version = version;
        this.versionGapThreshold = versionGapThreshold;
        this.port = port;
        this.id = id;
        this.keyManager = keyManager;
        this.self = self;

        this.executor = executor;
        this.sendQueuePool = sendQueuePool;
        this.receivedMailSink = receivedMailSink;
        this.connectionPool = connectionPool;
        this.keyLifetime = keyLifetime;
    }

    @Override
    public Void call() {
        LOG.log(Level.FINE, "{0}: こんにちは。", this.contactingConnection);

        try {
            subCall();
        } catch (final Exception e) {
            if (!Thread.currentThread().isInterrupted() && !this.contactingConnection.isClosed()) {
                // 別プロセスが接続を閉じて終了を報せてくれたわけでもない。
                LOG.log(Level.FINEST, "{0}: 異常発生: {1}", new Object[] { this.contactingConnection, e.toString() });
                ConcurrentFunctions.completePut(new ContactError(this.contactingConnection.getDestination(), e), this.messengerReportSink);
                errorAction();
            }
        } finally {
            // 登録の削除。
            this.contactingConnectionPool.remove(this.contactingConnection.getIdNumber());
        }

        LOG.log(Level.FINE, "{0}: さようなら。", this.contactingConnection);
        return null;
    }

    private void errorAction() {
        this.contactingConnection.close();
        // キューの削除。
        final List<List<Message>> remains = this.sendQueuePool.removeQueue(this.contactingConnection.getDestination(),
                this.contactingConnection.getType(), this.contactingConnection.getIdNumber());
        if (remains != null) {
            LOG.log(Level.FINEST, "{0}: 郵便ポストを破棄しました。", this.contactingConnection);
            if (!remains.isEmpty()) {
                ConcurrentFunctions.completePut(
                        new UnsentMail(this.contactingConnection.getDestination(), this.contactingConnection.getType(), remains), this.messengerReportSink);
            }
        }
    }

    private void updateSelf(final InetSocketAddress declaredSelf) {
        // 外聞の更新。
        final InetSocketAddress oldSelf = this.self.get();
        final InetSocketAddress newSelf = InetAddressFunctions.selectBetter(oldSelf, declaredSelf);
        if (!newSelf.equals(oldSelf)) {
            this.self.set(newSelf);
            ConcurrentFunctions.completePut(new SelfReport(newSelf), this.messengerReportSink);
        }
    }

    private void subCall() throws IOException, MyRuleException {

        // 受信の時間制限を設定。
        this.contactingConnection.getSocket().setSoTimeout((int) this.operationTimeout);

        // バッファサイズを設定。
        final int oldReceiveBufferSize = this.contactingConnection.getSocket().getReceiveBufferSize();
        final int oldSendBufferSize = this.contactingConnection.getSocket().getSendBufferSize();
        if (oldReceiveBufferSize < this.receiveBufferSize) {
            this.contactingConnection.getSocket().setReceiveBufferSize(this.receiveBufferSize);
            LOG.log(Level.FINEST, "{0}: 受信バッファサイズを {1} から {2} に変更しました。",
                    new Object[] { this.contactingConnection, Integer.toString(oldReceiveBufferSize), Integer.toString(this.receiveBufferSize) });
        }
        if (oldSendBufferSize < this.sendBufferSize) {
            this.contactingConnection.getSocket().setSendBufferSize(this.sendBufferSize);
            LOG.log(Level.FINEST, "{0}: 送信バッファサイズを {1} から {2} に変更しました。",
                    new Object[] { this.contactingConnection, Integer.toString(oldSendBufferSize), Integer.toString(this.sendBufferSize) });
        }

        if (Global.isDebug()) {
            this.contactingConnection.getSocket().setReuseAddress(true);
        }

        // つなげる。
        this.contactingConnection.getSocket().connect(
                new InetSocketAddress(this.contactingConnection.getDestination().getAddress(), this.contactingConnection.getDestination().getPort()),
                (int) this.operationTimeout);

        LOG.log(Level.FINEST, "{0}: 接触しました。", this.contactingConnection);

        final InputStream input = new BufferedInputStream(this.contactingConnection.getSocket().getInputStream(),
                this.contactingConnection.getSocket().getReceiveBufferSize());
        final OutputStream output = new BufferedOutputStream(this.contactingConnection.getSocket().getOutputStream(),
                this.contactingConnection.getSocket().getSendBufferSize());

        // 一言目の送信。
        final KeyPair keyPair = this.keyManager.getPublicKeyPair();
        StartingProtocol.sendFirst(this.transceiver, output, keyPair.getPublic());

        // 一言目への相槌を受信。
        final FirstReply reply1 = StartingProtocol.receiveFirstReply(this.transceiver, input, keyPair.getPrivate());
        final Key communicationKey = reply1.getKey();

        // 二言目の送信。
        final byte[] watchword = new byte[CryptographicKeys.PUBLIC_KEY_SIZE / Byte.SIZE / 2];
        ThreadLocalRandom.current().nextBytes(watchword);
        final InetSocketAddress destination = (InetSocketAddress) this.contactingConnection.getSocket().getRemoteSocketAddress();
        StartingProtocol.sendSecond(this.transceiver, output, this.id, communicationKey, watchword, this.version, this.port,
                this.contactingConnection.getType(), destination);

        // 二言目への相槌を受信。
        Message received;
        try {
            received = StartingProtocol.receiveSecondReply(this.transceiver, input, communicationKey);
        } catch (final SocketTimeoutException e) {
            // たぶんポート検査で弾かれた。
            ConcurrentFunctions.completePut(new ClosePortWarning(this.port), this.messengerReportSink);
            errorAction();
            return;
        }

        if (received instanceof PortErrorMessage) {
            // ポート検査で弾かれた。
            ConcurrentFunctions.completePut(new ClosePortWarning(this.port), this.messengerReportSink);
            errorAction();
            return;
        }
        final SecondReply reply2 = (SecondReply) received;
        final PublicKey destinationId = reply2.getId();
        final byte[] signedWatchword = reply2.getEncryptedWatchword();
        final PublicKey destinationPublicKey = reply2.getKey();
        final long destinationVersion = reply2.getVersion();
        final InetSocketAddress declaredSelf = reply2.getPeer();

        // 相手が識別用鍵の所持者かどうか検査。
        if (!Arrays.equals(watchword, CryptographicFunctions.decrypt(destinationId, signedWatchword))) {
            throw new MyRuleException("Invalid destinationPeer id.");
        }

        if (destinationVersion != this.version) {
            // 版が合わない。
            if (this.version < destinationVersion) {
                ConcurrentFunctions.completePut(new NewProtocolWarning(this.version, destinationVersion), this.messengerReportSink);
            } else {
                LOG.log(Level.FINEST, "{0}: 自分 ( 第 {1} 版 ) より古い個体 ( 第 {2} 版 ) を検知しました。",
                        new Object[] { this.contactingConnection, Long.toString(this.version), Long.toString(destinationVersion) });
            }

            if (Math.abs(this.version - destinationVersion) >= this.versionGapThreshold) {
                // 離れすぎ。
                errorAction();
                return;
            }
        }

        // 渡りをつけたので報告。
        updateSelf(declaredSelf);
        ConcurrentFunctions.completePut(new ConnectReport(destinationId, destination, this.contactingConnection.getType()), this.messengerReportSink);

        // 受信の時間制限を設定。
        this.contactingConnection.getSocket().setSoTimeout((int) this.connectionTimeout);

        // 本格的な送受信の開始。
        final Connection connection = new Connection(this.contactingConnection.getIdNumber(), this.contactingConnection.getDestination(), destinationId,
                this.contactingConnection.getType(), this.contactingConnection.getSocket());
        synchronized (this.connectionPool) {
            this.connectionPool.add(connection);
            this.sendQueuePool.addQueue(this.contactingConnection.getDestination(), this.contactingConnection.getType(),
                    this.contactingConnection.getIdNumber());
        }
        LOG.log(Level.FINER, "{0}: {1} との種別 {2} での通信を開始します。",
                new Object[] { this.contactingConnection, this.contactingConnection.getDestination(), Integer.toString(this.contactingConnection.getType()) });
        connection.setSender(this.executor.submit(new Sender(this.sendQueuePool, this.messengerReportSink, this.connectionPool, this.connectionTimeout,
                this.transceiver, connection, output, this.keyLifetime, keyPair.getPrivate(), destinationPublicKey, communicationKey)));
        this.executor.submit(new Receiver(this.receivedMailSink, this.messengerReportSink, this.connectionTimeout, this.transceiver, connection, input,
                keyPair.getPrivate(), destinationPublicKey, communicationKey));
    }

}
