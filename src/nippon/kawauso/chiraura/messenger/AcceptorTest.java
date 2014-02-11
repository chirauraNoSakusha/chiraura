/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.Key;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.test.SocketWrapper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class AcceptorTest {

    private static final Transceiver transceiver;
    static {
        final TypeRegistry<Message> registry = TypeRegistries.newRegistry();
        transceiver = new Transceiver(Integer.MAX_VALUE, RegistryInitializer.init(registry));
    }

    private static final int sendBufferSize = 128 * 1024;
    private static final long connectionTimeout = 10_000L;
    private static final long operationTimeout = 10_000L;
    private static final long keyLifetime = 10_000L;
    private static final long version = 1;
    private static final long versionGapThreshold = 1;

    private static final int connectionType = ConnectionTypes.DATA;

    private static final int testerPort = 12345;
    private static final KeyPair testerId = CryptographicKeys.newPublicKeyPair();
    private static final KeyPair testerPublicKeyPair = CryptographicKeys.newPublicKeyPair();

    private static final KeyPair subjectId = CryptographicKeys.newPublicKeyPair();

    private final ExecutorService executor;

    private final ServerSocket testerServerSocket;
    private final Socket testerSocket;
    private final InputStream testerInput;
    private final OutputStream testerOutput;

    private final ServerSocket subjectServerSocket;
    private final AcceptedConnection subjectConnection;

    private final BlockingQueue<ReceivedMail> subjectReceivedMailQueue;
    private final SendQueuePool subjectSendQueuePool;
    private final BlockingQueue<MessengerReport> subjectMessengerReportQueue;
    private final ConnectionPool<AcceptedConnection> subjectAcceptedConnectionPool;
    private final BoundConnectionPool<Connection> subjectConnectionPool;
    private final PublicKeyManager subjectKeyManager;
    private final AtomicReference<InetSocketAddress> subjectSelf;

    /**
     * 初期化。
     * @throws Exception 異常
     */
    public AcceptorTest() throws Exception {
        this.executor = Executors.newCachedThreadPool();

        this.testerServerSocket = new ServerSocket(testerPort);
        this.subjectServerSocket = new ServerSocket(testerPort + 1);
        this.testerSocket = new Socket(InetAddress.getLocalHost(), this.subjectServerSocket.getLocalPort());
        this.subjectConnection = new AcceptedConnection(1234, this.subjectServerSocket.accept());
        this.testerInput = new BufferedInputStream(this.testerSocket.getInputStream());
        this.testerOutput = new BufferedOutputStream(this.testerSocket.getOutputStream());

        this.subjectReceivedMailQueue = new LinkedBlockingQueue<>();
        this.subjectSendQueuePool = new BasicSendQueuePool();
        this.subjectMessengerReportQueue = new LinkedBlockingQueue<>();
        this.subjectAcceptedConnectionPool = new ConnectionPool<>();
        this.subjectConnectionPool = new BoundConnectionPool<>();
        this.subjectKeyManager = new PublicKeyManager(100_000L);
        this.subjectSelf = new AtomicReference<>(null);

        this.subjectAcceptedConnectionPool.add(this.subjectConnection);

        // DebugFunctions.startDebugLogging();
    }

    /**
     * 終処理。
     * @throws Exception 異常
     */
    @After
    public void tearDown() throws Exception {
        this.executor.shutdownNow();
        for (final Connection c : this.subjectConnectionPool.getAll()) {
            c.close();
        }
        this.testerInput.close();
        this.testerOutput.close();
        this.testerSocket.close();
        this.testerServerSocket.close();
        this.subjectServerSocket.close();
        Assert.assertTrue(this.executor.awaitTermination(1, TimeUnit.SECONDS));
        Assert.assertTrue(this.subjectAcceptedConnectionPool.isEmpty());
        for (final MessengerReport report : this.subjectMessengerReportQueue) {
            // isEmpty でないのはデバッグのため。
            Assert.fail(report.toString());
        }
        for (final ReceivedMail mail : this.subjectReceivedMailQueue) {
            // isEmpty でないのはデバッグのため。
            Assert.fail(mail.toString());
        }
    }

    /**
     * 正常系。
     * @throws Exception エラー
     */
    @Test
    public void testSample() throws Exception {
        final Acceptor instance = new Acceptor(this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool, sendBufferSize, connectionTimeout,
                operationTimeout, transceiver, this.subjectConnection, version, versionGapThreshold, subjectId, this.subjectKeyManager, this.subjectSelf,
                this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.subjectConnectionPool, keyLifetime);
        this.executor.submit(instance);

        // 一言目の送信。
        StartingProtocol.sendFirst(transceiver, this.testerOutput, testerPublicKeyPair.getPublic());

        // 一言目への相槌を受信。
        final FirstReply reply1 = StartingProtocol.receiveFirstReply(transceiver, this.testerInput, testerPublicKeyPair.getPrivate());
        final Key communicationKey = reply1.getKey();

        // 二言目の送信。
        final Random random = new Random();
        final byte[] watchword = new byte[CryptographicKeys.PUBLIC_KEY_SIZE / Byte.SIZE / 2];
        random.nextBytes(watchword);
        StartingProtocol.sendSecond(transceiver, this.testerOutput, testerId, communicationKey, watchword, version, testerPort, connectionType,
                (InetSocketAddress) this.testerSocket.getRemoteSocketAddress());

        // ポート検査への応答。
        try (final Socket socket = this.testerServerSocket.accept()) {
            final InputStream input = new BufferedInputStream(socket.getInputStream());
            final OutputStream output = new BufferedOutputStream(socket.getOutputStream());

            final PortCheckMessage portCheck = (PortCheckMessage) StartingProtocol.receiveFirst(transceiver, input);
            final byte[] keyBytes = CryptographicFunctions.decrypt(testerId.getPrivate(), portCheck.getEncryptedKey());
            final Key communicationKey2 = CryptographicKeys.getCommonKey(keyBytes);

            StartingProtocol.sendPortCheckReply(transceiver, output, communicationKey2);
        }

        // 二言目への相槌を受信。
        final SecondReply reply2 = (SecondReply) StartingProtocol.receiveSecondReply(transceiver, this.testerInput, communicationKey);

        Assert.assertEquals(subjectId.getPublic(), reply2.getId());
        Assert.assertArrayEquals(watchword, CryptographicFunctions.decrypt(reply2.getId(), reply2.getEncryptedWatchword()));

        // 報告の確認。
        final SelfReport selfReport = (SelfReport) this.subjectMessengerReportQueue.poll(1, TimeUnit.SECONDS);
        Assert.assertEquals(this.testerSocket.getRemoteSocketAddress(), selfReport.get());
        final ConnectReport connectReport = (ConnectReport) this.subjectMessengerReportQueue.poll(1, TimeUnit.SECONDS);
        Assert.assertEquals(testerId.getPublic(), connectReport.getDestinationId());
        Assert.assertEquals(testerPort, connectReport.getDestination().getPort());

        // Sender の稼動確認。
        final List<Message> sendMail = new ArrayList<>(1);
        sendMail.add(new TestMessage("あほやで"));
        this.subjectSendQueuePool.put(connectReport.getDestination(), connectionType, sendMail);

        final List<Message> recvMail = new ArrayList<>(1);
        transceiver.fromStream(this.testerInput, communicationKey, recvMail);
        Assert.assertEquals(sendMail, recvMail);

        // Receiver の稼動確認。
        sendMail.clear();
        sendMail.add(new TestMessage("あほかね"));
        transceiver.toStream(this.testerOutput, sendMail, EncryptedEnvelope.class, communicationKey);
        this.testerOutput.flush();

        final ReceivedMail receivedMail = this.subjectReceivedMailQueue.poll(1, TimeUnit.SECONDS);
        Assert.assertEquals(sendMail, receivedMail.getMail());

        // 接続が登録されているかどうか。
        Assert.assertEquals(1, this.subjectConnectionPool.get(receivedMail.getSourcePeer()).size());
        Assert.assertEquals(connectionType, this.subjectConnectionPool.get(receivedMail.getSourcePeer()).get(0).getType());
    }

    /**
     * 通信異常。
     * @throws Exception 異常
     */
    @Test
    public void testErrorStream() throws Exception {
        final int errorThreshold = 0;
        final Socket errorSocket = SocketWrapper.getErrorInputSocket(this.subjectConnection.getSocket(), errorThreshold);
        final AcceptedConnection errorConnection = new AcceptedConnection(this.subjectConnection.getIdNumber(), errorSocket);
        final Acceptor instance = new Acceptor(this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool, sendBufferSize, connectionTimeout,
                operationTimeout, transceiver, errorConnection, version, versionGapThreshold, subjectId, this.subjectKeyManager, this.subjectSelf,
                this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.subjectConnectionPool, keyLifetime);
        final Future<Void> future = this.executor.submit(instance);

        // 一言目を送信。
        StartingProtocol.sendFirst(transceiver, this.testerOutput, testerPublicKeyPair.getPublic());

        future.get(1, TimeUnit.SECONDS);

        final MessengerReport report = this.subjectMessengerReportQueue.poll(1, TimeUnit.SECONDS);
        Assert.assertTrue(report instanceof AcceptanceError);
        Assert.assertTrue(((AcceptanceError) report).getError() instanceof IOException);
        Assert.assertEquals(this.subjectConnection.getSocket().getInetAddress(), ((AcceptanceError) report).getDestination());
    }

    /**
     * メッセージ規約違反。
     * @throws Exception 異常
     */
    @Test
    public void testInvalidMail() throws Exception {
        final Acceptor instance = new Acceptor(this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool, sendBufferSize, connectionTimeout,
                operationTimeout, transceiver, this.subjectConnection, version, versionGapThreshold, subjectId, this.subjectKeyManager, this.subjectSelf,
                this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.subjectConnectionPool, keyLifetime);
        final Future<Void> future = this.executor.submit(instance);

        this.testerOutput.write(new byte[] { 0, 4, 1, 127, 1, 0 });
        this.testerOutput.flush();

        future.get(1, TimeUnit.SECONDS);

        final MessengerReport report = this.subjectMessengerReportQueue.poll(1, TimeUnit.SECONDS);
        Assert.assertTrue(report instanceof AcceptanceError);
        Assert.assertTrue(((AcceptanceError) report).getError() instanceof MyRuleException);
        Assert.assertEquals(this.subjectConnection.getSocket().getInetAddress(), ((AcceptanceError) report).getDestination());
    }

    /**
     * 時間切れ。
     * @throws Exception エラー
     */
    @Test
    public void testTimeout() throws Exception {
        final long shortOperationTimeout = 100L;
        final Acceptor instance = new Acceptor(this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool, sendBufferSize, operationTimeout,
                shortOperationTimeout, transceiver, this.subjectConnection, version, versionGapThreshold, subjectId, this.subjectKeyManager, this.subjectSelf,
                this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.subjectConnectionPool, keyLifetime);
        final Future<Void> future = this.executor.submit(instance);

        // 時間切れ待ち。
        future.get(shortOperationTimeout + 100L, TimeUnit.MILLISECONDS);

        final MessengerReport report = this.subjectMessengerReportQueue.poll(1, TimeUnit.SECONDS);
        Assert.assertTrue(report instanceof AcceptanceError);
        Assert.assertTrue(((AcceptanceError) report).getError() instanceof SocketTimeoutException);
        Assert.assertEquals(this.subjectConnection.getSocket().getInetAddress(), ((AcceptanceError) report).getDestination());
    }

    /**
     * ポート異常。
     * @throws Exception エラー
     */
    @Test
    public void testPortError() throws Exception {
        final Acceptor instance = new Acceptor(this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool, sendBufferSize, operationTimeout,
                operationTimeout, transceiver, this.subjectConnection, version, versionGapThreshold, subjectId, this.subjectKeyManager, this.subjectSelf,
                this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.subjectConnectionPool, keyLifetime);
        final Future<Void> future = this.executor.submit(instance);

        this.testerServerSocket.close();

        // 一言目の送信。
        StartingProtocol.sendFirst(transceiver, this.testerOutput, testerPublicKeyPair.getPublic());

        // 一言目への相槌を受信。
        final FirstReply reply1 = StartingProtocol.receiveFirstReply(transceiver, this.testerInput, testerPublicKeyPair.getPrivate());
        final Key communicationKey = reply1.getKey();

        // LoggingFunctions.startDebugLogging();

        // 二言目の送信。
        final Random random = new Random();
        final byte[] watchword = new byte[CryptographicKeys.PUBLIC_KEY_SIZE / Byte.SIZE / 2];
        random.nextBytes(watchword);
        StartingProtocol.sendSecond(transceiver, this.testerOutput, testerId, communicationKey, watchword, version, testerPort, connectionType,
                (InetSocketAddress) this.testerSocket.getRemoteSocketAddress());

        // ポート異常を受信。
        this.testerSocket.setSoTimeout((int) (operationTimeout + 1_000L));
        future.get();
        final PortErrorMessage reply2 = (PortErrorMessage) StartingProtocol.receiveSecondReply(transceiver, this.testerInput, communicationKey);
        Assert.assertNotNull(reply2);
    }

    /**
     * しょぼいバージョン。
     * @throws Exception エラー
     */
    @Test
    public void testOldVersion() throws Exception {
        final Acceptor instance = new Acceptor(this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool, sendBufferSize, connectionTimeout,
                operationTimeout, transceiver, this.subjectConnection, version, versionGapThreshold, subjectId, this.subjectKeyManager, this.subjectSelf,
                this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.subjectConnectionPool, keyLifetime);
        this.executor.submit(instance);

        // 一言目の送信。
        StartingProtocol.sendFirst(transceiver, this.testerOutput, testerPublicKeyPair.getPublic());

        // 一言目への相槌を受信。
        final FirstReply reply1 = StartingProtocol.receiveFirstReply(transceiver, this.testerInput, testerPublicKeyPair.getPrivate());
        final Key communicationKey = reply1.getKey();

        // 二言目の送信。
        final Random random = new Random();
        final byte[] watchword = new byte[CryptographicKeys.PUBLIC_KEY_SIZE / Byte.SIZE / 2];
        random.nextBytes(watchword);
        StartingProtocol.sendSecond(transceiver, this.testerOutput, testerId, communicationKey, watchword, version - 1, testerPort, connectionType,
                (InetSocketAddress) this.testerSocket.getRemoteSocketAddress());

        // 二言目への相槌を受信。
        StartingProtocol.receiveSecondReply(transceiver, this.testerInput, communicationKey);

        // 切断待ち。
        Assert.assertEquals(-1, this.testerInput.read());

        // 接続が登録されているかどうか。
        Assert.assertTrue(this.subjectConnectionPool.isEmpty());
    }

    /**
     * 新しいバージョン。
     * @throws Exception エラー
     */
    @Test
    public void testNewVersion() throws Exception {
        final Acceptor instance = new Acceptor(this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool, sendBufferSize, connectionTimeout,
                operationTimeout, transceiver, this.subjectConnection, version, versionGapThreshold, subjectId, this.subjectKeyManager, this.subjectSelf,
                this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.subjectConnectionPool, keyLifetime);
        this.executor.submit(instance);

        // 一言目の送信。
        StartingProtocol.sendFirst(transceiver, this.testerOutput, testerPublicKeyPair.getPublic());

        // 一言目への相槌を受信。
        final FirstReply reply1 = StartingProtocol.receiveFirstReply(transceiver, this.testerInput, testerPublicKeyPair.getPrivate());
        final Key communicationKey = reply1.getKey();

        // 二言目の送信。
        final Random random = new Random();
        final byte[] watchword = new byte[CryptographicKeys.PUBLIC_KEY_SIZE / Byte.SIZE / 2];
        random.nextBytes(watchword);
        StartingProtocol.sendSecond(transceiver, this.testerOutput, testerId, communicationKey, watchword, version + versionGapThreshold, testerPort,
                connectionType, (InetSocketAddress) this.testerSocket.getRemoteSocketAddress());

        // 二言目への相槌を受信。
        StartingProtocol.receiveSecondReply(transceiver, this.testerInput, communicationKey);

        // 切断待ち。
        Assert.assertEquals(-1, this.testerInput.read());

        // 報告の確認。
        final NewProtocolWarning newProtocol = (NewProtocolWarning) this.subjectMessengerReportQueue.poll(1, TimeUnit.SECONDS);
        Assert.assertEquals(version, newProtocol.getVersion());
        Assert.assertEquals(version + 1, newProtocol.getNewVersion());
    }

}
