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

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.connection.BasicConstantTrafficLimiter;
import nippon.kawauso.chiraura.lib.connection.Limiter;
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

    private static final Transceiver.Share transceiverShare;
    private static final boolean http = Global.useHttpWrapper();
    static {
        final TypeRegistry<Message> registry = TypeRegistries.newRegistry();
        transceiverShare = new Transceiver.Share(Integer.MAX_VALUE, http, RegistryInitializer.init(registry));
    }
    private static final boolean portIgnore = false;
    private static final int connectionLimit = 5;
    private static final long duration = Duration.SECOND / 2;
    private static final long sizeLimit = 10_000_000L;
    private static final int countLimit = 1_000;
    private static final long penalty = 5 * Duration.SECOND;
    private final Limiter<InetSocketAddress> limiter = new BasicConstantTrafficLimiter(duration, sizeLimit, countLimit, penalty);

    private static final int sendBufferSize = 128 * 1024;
    private static final long connectionTimeout = 10 * Duration.SECOND;
    private static final long operationTimeout = 10 * Duration.SECOND;
    private static final long keyLifetime = 10 * Duration.SECOND;
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
    private final ConnectionPool<Connection> subjectConnectionPool;
    private final PublicKeyManager subjectKeyManager;
    private final AtomicReference<InetSocketAddress> subjectSelf;

    /**
     * 初期化。
     * @throws Exception 異常
     */
    public AcceptorTest() throws Exception {
        this.executor = Executors.newCachedThreadPool();

        this.testerServerSocket = new ServerSocket();
        this.testerServerSocket.setReuseAddress(true);
        this.testerServerSocket.bind(new InetSocketAddress(testerPort));
        this.subjectServerSocket = new ServerSocket();
        this.subjectServerSocket.setReuseAddress(true);
        this.subjectServerSocket.bind(new InetSocketAddress(testerPort + 1));
        this.testerSocket = new Socket(InetAddress.getLocalHost(), this.subjectServerSocket.getLocalPort());
        this.subjectConnection = new AcceptedConnection(1234, this.subjectServerSocket.accept());
        this.testerInput = new BufferedInputStream(this.testerSocket.getInputStream());
        this.testerOutput = new BufferedOutputStream(this.testerSocket.getOutputStream());

        this.subjectReceivedMailQueue = new LinkedBlockingQueue<>();
        this.subjectSendQueuePool = new BasicSendQueuePool();
        this.subjectMessengerReportQueue = new LinkedBlockingQueue<>();
        this.subjectAcceptedConnectionPool = new PortIgnoringConnectionPool<>();
        this.subjectConnectionPool = new BoundConnectionPool<>();
        this.subjectKeyManager = new PublicKeyManager(100 * Duration.SECOND);
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
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        Assert.assertEquals(new ArrayList<AcceptedConnection>(0), this.subjectAcceptedConnectionPool.getAll());
        Assert.assertNull(this.subjectMessengerReportQueue.poll());
        Assert.assertNull(this.subjectReceivedMailQueue.poll());
    }

    /**
     * 正常系。
     * @throws Exception エラー
     */
    @Test
    public void testSample() throws Exception {
        final Acceptor instance = new Acceptor(portIgnore, connectionLimit, this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool,
                sendBufferSize, connectionTimeout, operationTimeout, transceiverShare, this.subjectConnection, version, versionGapThreshold, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        this.executor.submit(instance);

        final Transceiver testerTransceiver = new Transceiver(transceiverShare, this.testerInput, this.testerOutput, new InetSocketAddress(
                this.subjectServerSocket.getLocalPort()));

        // 一言目の送信。
        StartingProtocol.sendFirst(testerTransceiver, testerPublicKeyPair.getPublic());

        // 一言目への相槌を受信。
        final FirstReply reply1 = StartingProtocol.receiveFirstReply(testerTransceiver, testerPublicKeyPair.getPrivate());
        final Key communicationKey = reply1.getKey();

        // 二言目の送信。
        final Random random = new Random();
        final byte[] watchword = new byte[CryptographicKeys.PUBLIC_KEY_SIZE / Byte.SIZE / 2];
        random.nextBytes(watchword);
        StartingProtocol.sendSecond(testerTransceiver, testerId, communicationKey, watchword, version, testerPort, connectionType,
                (InetSocketAddress) this.testerSocket.getRemoteSocketAddress());

        // ポート検査への応答。
        try (final Socket socket = this.testerServerSocket.accept()) {
            final InputStream input = new BufferedInputStream(socket.getInputStream());
            final OutputStream output = new BufferedOutputStream(socket.getOutputStream());
            final Transceiver testerTransceiver2 = new Transceiver(transceiverShare, input, output, null);

            final PortCheckMessage portCheck = (PortCheckMessage) StartingProtocol.receiveFirst(testerTransceiver2);
            final byte[] keyBytes = CryptographicFunctions.decrypt(testerId.getPrivate(), portCheck.getEncryptedKey());
            final Key communicationKey2 = CryptographicKeys.getCommonKey(keyBytes);

            StartingProtocol.sendPortCheckReply(testerTransceiver2, communicationKey2);
        }

        // 二言目への相槌を受信。
        final SecondReply reply2 = (SecondReply) StartingProtocol.receiveSecondReply(testerTransceiver, communicationKey);

        Assert.assertEquals(subjectId.getPublic(), reply2.getId());
        Assert.assertArrayEquals(watchword, CryptographicFunctions.decrypt(reply2.getId(), reply2.getEncryptedWatchword()));

        // 報告の確認。
        final SelfReport selfReport = (SelfReport) this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertEquals(this.testerSocket.getRemoteSocketAddress(), selfReport.getSelf());
        final ConnectReport connectReport = (ConnectReport) this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertEquals(testerId.getPublic(), connectReport.getDestinationId());
        Assert.assertEquals(testerPort, connectReport.getDestination().getPort());

        // Sender の稼動確認。
        final List<Message> sendMail = new ArrayList<>(1);
        sendMail.add(new TestMessage("あほやで"));
        this.subjectSendQueuePool.put(connectReport.getDestination(), connectionType, sendMail);

        final List<Message> recvMail = new ArrayList<>(1);
        testerTransceiver.fromStream(communicationKey, recvMail);
        Assert.assertEquals(sendMail, recvMail);

        // Receiver の稼動確認。
        sendMail.clear();
        sendMail.add(new TestMessage("あほかね"));
        testerTransceiver.toStream(sendMail, EncryptedEnvelope.class, communicationKey);
        this.testerOutput.flush();

        final ReceivedMail receivedMail = this.subjectReceivedMailQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
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
        final Acceptor instance = new Acceptor(portIgnore, connectionLimit, this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool,
                sendBufferSize, connectionTimeout, operationTimeout, transceiverShare, errorConnection, version, versionGapThreshold, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        final Future<Void> future = this.executor.submit(instance);

        final Transceiver testerTransceiver = new Transceiver(transceiverShare, this.testerInput, this.testerOutput, new InetSocketAddress(
                this.subjectServerSocket.getLocalPort()));

        // 一言目を送信。
        StartingProtocol.sendFirst(testerTransceiver, testerPublicKeyPair.getPublic());

        future.get(Duration.SECOND, TimeUnit.MILLISECONDS);

        final MessengerReport report = this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertTrue(report instanceof AcceptanceError);
        Assert.assertTrue(((AcceptanceError) report).getError() instanceof IOException);
        Assert.assertEquals(this.subjectConnection.getDestination(), ((AcceptanceError) report).getDestination());
    }

    /**
     * メッセージ規約違反。
     * @throws Exception 異常
     */
    @Test
    public void testInvalidMail() throws Exception {
        final Acceptor instance = new Acceptor(portIgnore, connectionLimit, this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool,
                sendBufferSize, connectionTimeout, operationTimeout, transceiverShare, this.subjectConnection, version, versionGapThreshold, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        final Future<Void> future = this.executor.submit(instance);

        this.testerOutput.write(new byte[] { 0, 4, 1, 127, 1, 0 });
        this.testerOutput.flush();

        future.get(Duration.SECOND, TimeUnit.MILLISECONDS);

        final MessengerReport report = this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertTrue(report instanceof AcceptanceError);
        Assert.assertTrue(((AcceptanceError) report).getError() instanceof MyRuleException);
        Assert.assertEquals(this.subjectConnection.getDestination(), ((AcceptanceError) report).getDestination());
    }

    /**
     * 時間切れ。
     * @throws Exception エラー
     */
    @Test
    public void testTimeout() throws Exception {
        final long shortOperationTimeout = 100L;
        final Acceptor instance = new Acceptor(portIgnore, connectionLimit, this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool,
                sendBufferSize, operationTimeout, shortOperationTimeout, transceiverShare, this.subjectConnection, version, versionGapThreshold, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        final Future<Void> future = this.executor.submit(instance);

        // 時間切れ待ち。
        future.get(shortOperationTimeout + 100L, TimeUnit.MILLISECONDS);

        final MessengerReport report = this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertTrue(report instanceof AcceptanceError);
        Assert.assertTrue(((AcceptanceError) report).getError() instanceof SocketTimeoutException);
        Assert.assertEquals(this.subjectConnection.getDestination(), ((AcceptanceError) report).getDestination());
    }

    /**
     * ポート異常。
     * @throws Exception エラー
     */
    @Test
    public void testPortError() throws Exception {
        final Acceptor instance = new Acceptor(portIgnore, connectionLimit, this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool,
                sendBufferSize, operationTimeout, operationTimeout, transceiverShare, this.subjectConnection, version, versionGapThreshold, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        final Future<Void> future = this.executor.submit(instance);

        this.testerServerSocket.close();

        final Transceiver testerTransceiver = new Transceiver(transceiverShare, this.testerInput, this.testerOutput, new InetSocketAddress(
                this.subjectServerSocket.getLocalPort()));

        // 一言目の送信。
        StartingProtocol.sendFirst(testerTransceiver, testerPublicKeyPair.getPublic());

        // 一言目への相槌を受信。
        final FirstReply reply1 = StartingProtocol.receiveFirstReply(testerTransceiver, testerPublicKeyPair.getPrivate());
        final Key communicationKey = reply1.getKey();

        // LoggingFunctions.startDebugLogging();

        // 二言目の送信。
        final Random random = new Random();
        final byte[] watchword = new byte[CryptographicKeys.PUBLIC_KEY_SIZE / Byte.SIZE / 2];
        random.nextBytes(watchword);
        StartingProtocol.sendSecond(testerTransceiver, testerId, communicationKey, watchword, version, testerPort, connectionType,
                (InetSocketAddress) this.testerSocket.getRemoteSocketAddress());

        // ポート異常を受信。
        this.testerSocket.setSoTimeout((int) (operationTimeout + Duration.SECOND));
        future.get();
        final PortErrorMessage reply2 = (PortErrorMessage) StartingProtocol.receiveSecondReply(testerTransceiver, communicationKey);
        Assert.assertNotNull(reply2);
    }

    /**
     * しょぼいバージョン。
     * @throws Exception エラー
     */
    @Test
    public void testOldVersion() throws Exception {
        final Acceptor instance = new Acceptor(portIgnore, connectionLimit, this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool,
                sendBufferSize, connectionTimeout, operationTimeout, transceiverShare, this.subjectConnection, version, versionGapThreshold, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        this.executor.submit(instance);

        final Transceiver testerTransceiver = new Transceiver(transceiverShare, this.testerInput, this.testerOutput, new InetSocketAddress(
                this.subjectServerSocket.getLocalPort()));
        // 一言目の送信。
        StartingProtocol.sendFirst(testerTransceiver, testerPublicKeyPair.getPublic());

        // 一言目への相槌を受信。
        final FirstReply reply1 = StartingProtocol.receiveFirstReply(testerTransceiver, testerPublicKeyPair.getPrivate());
        final Key communicationKey = reply1.getKey();

        // 二言目の送信。
        final Random random = new Random();
        final byte[] watchword = new byte[CryptographicKeys.PUBLIC_KEY_SIZE / Byte.SIZE / 2];
        random.nextBytes(watchword);
        StartingProtocol.sendSecond(testerTransceiver, testerId, communicationKey, watchword, version - 1, testerPort, connectionType,
                (InetSocketAddress) this.testerSocket.getRemoteSocketAddress());

        // 二言目への相槌を受信。
        StartingProtocol.receiveSecondReply(testerTransceiver, communicationKey);

        // 切断待ち。
        Assert.assertEquals(-1, this.testerInput.read());

        // 接続が登録されているかどうか。
        Assert.assertEquals(new ArrayList<Connection>(0), this.subjectConnectionPool.getAll());
    }

    /**
     * 新しいバージョン。
     * @throws Exception エラー
     */
    @Test
    public void testNewVersion() throws Exception {
        final Acceptor instance = new Acceptor(portIgnore, connectionLimit, this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool,
                sendBufferSize, connectionTimeout, operationTimeout, transceiverShare, this.subjectConnection, version, versionGapThreshold, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        this.executor.submit(instance);

        final Transceiver testerTransceiver = new Transceiver(transceiverShare, this.testerInput, this.testerOutput, new InetSocketAddress(
                this.subjectServerSocket.getLocalPort()));

        // 一言目の送信。
        StartingProtocol.sendFirst(testerTransceiver, testerPublicKeyPair.getPublic());

        // 一言目への相槌を受信。
        final FirstReply reply1 = StartingProtocol.receiveFirstReply(testerTransceiver, testerPublicKeyPair.getPrivate());
        final Key communicationKey = reply1.getKey();

        // 二言目の送信。
        final Random random = new Random();
        final byte[] watchword = new byte[CryptographicKeys.PUBLIC_KEY_SIZE / Byte.SIZE / 2];
        random.nextBytes(watchword);
        StartingProtocol.sendSecond(testerTransceiver, testerId, communicationKey, watchword, version + versionGapThreshold, testerPort,
                connectionType, (InetSocketAddress) this.testerSocket.getRemoteSocketAddress());

        // 二言目への相槌を受信。
        StartingProtocol.receiveSecondReply(testerTransceiver, communicationKey);

        // 切断待ち。
        Assert.assertEquals(-1, this.testerInput.read());

        // 報告の確認。
        final NewProtocolWarning newProtocol = (NewProtocolWarning) this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertEquals(version, newProtocol.getVersion());
        Assert.assertEquals(version + 1, newProtocol.getNewVersion());
    }

    /**
     * 接続制限。
     * @throws Exception エラー
     */
    @Test
    public void testConnectionLimit() throws Exception {
        for (int i = 0; i < connectionLimit; i++) {
            final AcceptedConnection connection = new AcceptedConnection(this.subjectConnection.getIdNumber() + i + 1, this.subjectConnection.getSocket());
            this.subjectAcceptedConnectionPool.add(connection);
        }

        final Acceptor instance = new Acceptor(true, connectionLimit, this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool,
                sendBufferSize, connectionTimeout, operationTimeout, transceiverShare, this.subjectConnection, version, versionGapThreshold, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        this.executor.submit(instance);

        Assert.assertEquals(-1, this.testerInput.read(new byte[1]));

        for (int i = 0; i < connectionLimit; i++) {
            this.subjectAcceptedConnectionPool.remove(this.subjectConnection.getIdNumber() + i + 1);
        }

        Thread.sleep(1L);

        final ConnectionOverflow report = (ConnectionOverflow) this.subjectMessengerReportQueue.poll();
        Assert.assertNotNull(report);
    }
}
