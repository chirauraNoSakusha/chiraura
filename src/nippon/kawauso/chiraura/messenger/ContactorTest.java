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
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.connection.Limiter;
import nippon.kawauso.chiraura.lib.connection.PortIgnoringConstantTrafficLimiter;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class ContactorTest {

    private static final Transceiver.Share transceiverShare;
    private static final boolean http = Global.useHttpWrapper();
    static {
        final TypeRegistry<Message> registry = TypeRegistries.newRegistry();
        transceiverShare = new Transceiver.Share(Integer.MAX_VALUE, http, RegistryInitializer.init(registry));
    }
    private static final long duration = Duration.SECOND / 2;
    private static final long sizeLimit = 10_000_000L;
    private static final int countLimit = 1_000;
    private static final long penalty = 5 * Duration.SECOND;
    private final Limiter<InetSocketAddress> limiter = new PortIgnoringConstantTrafficLimiter(duration, sizeLimit, countLimit, penalty);

    private static final int receiveBufferSize = 128 * 1024;
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

    private static final int subjectPort = testerPort + 10;
    private static final KeyPair subjectId = CryptographicKeys.newPublicKeyPair();

    private final ExecutorService executor;

    private final ServerSocket testerServerSocket;

    private final ContactingConnection subjectConnection;

    private final BlockingQueue<ReceivedMail> subjectReceivedMailQueue;
    private final SendQueuePool subjectSendQueuePool;
    private final BlockingQueue<MessengerReport> subjectMessengerReportQueue;
    private final ConnectionPool<ContactingConnection> subjectContactingConnectionPool;
    private final ConnectionPool<Connection> subjectConnectionPool;

    private final PublicKeyManager subjectKeyManager;

    private final AtomicReference<InetSocketAddress> subjectSelf;

    /**
     * 初期化。
     * @throws Exception 異常
     */
    public ContactorTest() throws Exception {
        this.testerServerSocket = new ServerSocket();
        this.testerServerSocket.setReuseAddress(true);
        this.testerServerSocket.bind(new InetSocketAddress(testerPort));

        this.subjectConnection = new ContactingConnection(1234, new InetSocketAddress(InetAddress.getLocalHost(), testerPort), connectionType);
        this.subjectKeyManager = new PublicKeyManager(100 * Duration.SECOND);

        this.subjectContactingConnectionPool = new BoundConnectionPool<>();
        this.subjectConnectionPool = new BoundConnectionPool<>();
        this.subjectReceivedMailQueue = new LinkedBlockingQueue<>();
        this.subjectSendQueuePool = new BasicSendQueuePool();
        this.subjectMessengerReportQueue = new LinkedBlockingQueue<>();

        this.executor = Executors.newCachedThreadPool();

        this.subjectContactingConnectionPool.add(this.subjectConnection);

        this.subjectSelf = new AtomicReference<>(null);
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
        this.testerServerSocket.close();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        Assert.assertEquals(new ArrayList<ContactingConnection>(0), this.subjectContactingConnectionPool.getAll());
        for (final MessengerReport report : this.subjectMessengerReportQueue) {
            Assert.fail(report.toString());
        }
    }

    /**
     * 正常系。
     * @throws Exception エラー
     */
    @Test
    public void testSample() throws Exception {
        final Contactor instance = new Contactor(this.subjectMessengerReportQueue, this.subjectContactingConnectionPool, receiveBufferSize, sendBufferSize,
                connectionTimeout, operationTimeout, transceiverShare, this.subjectConnection, version, versionGapThreshold, subjectPort, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        this.executor.submit(instance);

        // 接続受け付け。
        final Socket testerSocket = this.testerServerSocket.accept();
        testerSocket.setSoTimeout((int) connectionTimeout);
        final InputStream testerInput = new BufferedInputStream(testerSocket.getInputStream());
        final OutputStream testerOutput = new BufferedOutputStream(testerSocket.getOutputStream());
        final Transceiver testerTransceiver = new Transceiver(transceiverShare, testerInput, testerOutput, null);

        // 一言目を受信。
        final FirstMessage message1 = (FirstMessage) StartingProtocol.receiveFirst(testerTransceiver);
        final PublicKey subjectPublicKey = message1.getKey();

        // 一言目への相槌を送信。
        final Key communicationKey = CryptographicKeys.newCommonKey();
        StartingProtocol.sendFirstReply(testerTransceiver, subjectPublicKey, communicationKey);

        // 二言目を受信。
        final SecondMessage message2 = StartingProtocol.receiveSecond(testerTransceiver, communicationKey);
        Assert.assertEquals(subjectId.getPublic(), message2.getId());
        Assert.assertArrayEquals(communicationKey.getEncoded(), CryptographicFunctions.decrypt(message2.getId(), message2.getEncryptedKey()));
        final byte[] watchword = message2.getWatchword();
        Assert.assertEquals(subjectPort, message2.getPort());
        Assert.assertEquals(connectionType, message2.getType());

        // 二言目への相槌を送信。
        final InetSocketAddress subject = new InetSocketAddress(testerSocket.getInetAddress(), subjectPort);
        StartingProtocol.sendSecondReply(testerTransceiver, communicationKey, testerId, watchword, testerPublicKeyPair.getPublic(), version, subject);

        // 報告の確認。
        final SelfReport selfReport = (SelfReport) this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertEquals(subject, selfReport.getSelf());
        final ConnectReport connectReport = (ConnectReport) this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertEquals(testerId.getPublic(), connectReport.getDestinationId());
        Assert.assertEquals(testerPort, connectReport.getDestination().getPort());

        // 検査インスタンス側 Sender の稼動確認。
        final List<Message> sendMail = new ArrayList<>(1);
        sendMail.add(new TestMessage("あほやで"));
        this.subjectSendQueuePool.put(this.subjectConnection.getDestination(), connectionType, sendMail);

        final List<Message> recvMail = new ArrayList<>(1);
        testerTransceiver.fromStream(communicationKey, recvMail);
        Assert.assertEquals(sendMail, recvMail);

        // 検査インスタンス側 Receiver の稼動確認。
        sendMail.clear();
        sendMail.add(new TestMessage("あほかね"));
        testerTransceiver.toStream(sendMail, EncryptedEnvelope.class, communicationKey);
        testerOutput.flush();

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
        final Contactor instance = new Contactor(this.subjectMessengerReportQueue, this.subjectContactingConnectionPool, receiveBufferSize, sendBufferSize,
                connectionTimeout, operationTimeout, transceiverShare, this.subjectConnection, version, versionGapThreshold, subjectPort, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        final Future<Void> future = this.executor.submit(instance);

        this.testerServerSocket.close();

        future.get(Duration.SECOND, TimeUnit.MILLISECONDS);

        final MessengerReport report = this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertTrue(report instanceof ContactError);
        Assert.assertTrue(((ContactError) report).getError() instanceof IOException);
    }

    /**
     * メッセージ規約違反。
     * @throws Exception 異常
     */
    @Test
    public void testInvalidMail() throws Exception {
        final Contactor instance = new Contactor(this.subjectMessengerReportQueue, this.subjectContactingConnectionPool, receiveBufferSize, sendBufferSize,
                connectionTimeout, operationTimeout, transceiverShare, this.subjectConnection, version, versionGapThreshold, subjectPort, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        final Future<Void> future = this.executor.submit(instance);

        // 接続受け付け。
        final Socket testerSocket = this.testerServerSocket.accept();
        testerSocket.setSoTimeout((int) connectionTimeout);
        final InputStream testerInput = new BufferedInputStream(testerSocket.getInputStream());
        final OutputStream testerOutput = new BufferedOutputStream(testerSocket.getOutputStream());
        final Transceiver testerTransceiver = new Transceiver(transceiverShare, testerInput, testerOutput, null);

        // 一言目を受信。
        final FirstMessage message1 = (FirstMessage) StartingProtocol.receiveFirst(testerTransceiver);
        Assert.assertNotNull(message1);

        // 一言目への相槌の送信。
        testerOutput.write(new byte[] { 0, 4, 1, 127, 1, 0 });
        testerOutput.flush();

        future.get(Duration.SECOND, TimeUnit.MILLISECONDS);

        final MessengerReport report = this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertTrue(report instanceof ContactError);
        Assert.assertTrue(((ContactError) report).getError() instanceof MyRuleException);
        Assert.assertEquals(this.subjectConnection.getDestination(), ((ContactError) report).getDestination());
    }

    /**
     * 時間切れ。
     * @throws Exception エラー
     */
    @Test
    public void testTimeout() throws Exception {
        final long shortOperationTimeout = 100L;
        final Contactor instance = new Contactor(this.subjectMessengerReportQueue, this.subjectContactingConnectionPool, receiveBufferSize, sendBufferSize,
                connectionTimeout, shortOperationTimeout, transceiverShare, this.subjectConnection, version, versionGapThreshold, subjectPort, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        final Future<Void> future = this.executor.submit(instance);

        // 時間切れ待ち。
        future.get(shortOperationTimeout + Duration.SECOND, TimeUnit.MILLISECONDS);

        final MessengerReport report = this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertTrue(report instanceof ContactError);
        Assert.assertTrue(((ContactError) report).getError() instanceof SocketTimeoutException);
        Assert.assertEquals(this.subjectConnection.getDestination(), ((ContactError) report).getDestination());
    }

    /**
     * ポート異常。
     * @throws Exception エラー
     */
    @Test
    public void testPortError() throws Exception {
        final Contactor instance = new Contactor(this.subjectMessengerReportQueue, this.subjectContactingConnectionPool, receiveBufferSize, sendBufferSize,
                connectionTimeout, operationTimeout, transceiverShare, this.subjectConnection, version, versionGapThreshold, subjectPort, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        this.executor.submit(instance);

        // 接続受け付け。
        final Socket testerSocket = this.testerServerSocket.accept();
        testerSocket.setSoTimeout((int) connectionTimeout);
        final InputStream testerInput = new BufferedInputStream(testerSocket.getInputStream());
        final OutputStream testerOutput = new BufferedOutputStream(testerSocket.getOutputStream());
        final Transceiver testerTransceiver = new Transceiver(transceiverShare, testerInput, testerOutput, null);

        // 一言目を受信。
        final FirstMessage message1 = (FirstMessage) StartingProtocol.receiveFirst(testerTransceiver);
        final PublicKey subjectPublicKey = message1.getKey();

        // 一言目への相槌を送信。
        final Key communicationKey = CryptographicKeys.newCommonKey();
        StartingProtocol.sendFirstReply(testerTransceiver, subjectPublicKey, communicationKey);

        // 二言目を受信。
        final SecondMessage message2 = StartingProtocol.receiveSecond(testerTransceiver, communicationKey);
        Assert.assertEquals(subjectId.getPublic(), message2.getId());
        Assert.assertArrayEquals(communicationKey.getEncoded(), CryptographicFunctions.decrypt(message2.getId(), message2.getEncryptedKey()));
        Assert.assertEquals(subjectPort, message2.getPort());
        Assert.assertEquals(connectionType, message2.getType());

        // 二言目への相槌を送信。
        StartingProtocol.sendPortError(testerTransceiver, communicationKey);

        // 切断待ち。
        Assert.assertEquals(-1, testerInput.read());

        // 報告の確認。
        final ClosePortWarning closePort = (ClosePortWarning) this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertEquals(subjectPort, closePort.getPort());
    }

    /**
     * しょぼいバージョン。
     * @throws Exception エラー
     */
    @Test
    public void testOldVersion() throws Exception {
        final Contactor instance = new Contactor(this.subjectMessengerReportQueue, this.subjectContactingConnectionPool, receiveBufferSize, sendBufferSize,
                connectionTimeout, operationTimeout, transceiverShare, this.subjectConnection, version, versionGapThreshold, subjectPort, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        this.executor.submit(instance);

        // 接続受け付け。
        final Socket testerSocket = this.testerServerSocket.accept();
        testerSocket.setSoTimeout((int) connectionTimeout);
        final InputStream testerInput = new BufferedInputStream(testerSocket.getInputStream());
        final OutputStream testerOutput = new BufferedOutputStream(testerSocket.getOutputStream());
        final Transceiver testerTransceiver = new Transceiver(transceiverShare, testerInput, testerOutput, null);

        // 一言目を受信。
        final FirstMessage message1 = (FirstMessage) StartingProtocol.receiveFirst(testerTransceiver);
        final PublicKey subjectPublicKey = message1.getKey();

        // 一言目への相槌を送信。
        final Key communicationKey = CryptographicKeys.newCommonKey();
        StartingProtocol.sendFirstReply(testerTransceiver, subjectPublicKey, communicationKey);

        // 二言目を受信。
        final SecondMessage message2 = StartingProtocol.receiveSecond(testerTransceiver, communicationKey);
        Assert.assertEquals(subjectId.getPublic(), message2.getId());
        Assert.assertArrayEquals(communicationKey.getEncoded(), CryptographicFunctions.decrypt(message2.getId(), message2.getEncryptedKey()));
        final byte[] watchword = message2.getWatchword();
        Assert.assertEquals(subjectPort, message2.getPort());
        Assert.assertEquals(connectionType, message2.getType());

        // 二言目への相槌を送信。
        final InetSocketAddress subject = new InetSocketAddress(testerSocket.getInetAddress(), subjectPort);
        StartingProtocol.sendSecondReply(testerTransceiver, communicationKey, testerId, watchword, testerPublicKeyPair.getPublic(), version - 1, subject);

        // 切断待ち。
        Assert.assertEquals(-1, testerInput.read());

        // 接続が登録されているかどうか。
        Assert.assertEquals(new ArrayList<Connection>(0), this.subjectConnectionPool.getAll());
    }

    /**
     * 新しいバージョン。
     * @throws Exception エラー
     */
    @Test
    public void testNewVersion() throws Exception {
        final Contactor instance = new Contactor(this.subjectMessengerReportQueue, this.subjectContactingConnectionPool, receiveBufferSize, sendBufferSize,
                connectionTimeout, operationTimeout, transceiverShare, this.subjectConnection, version, versionGapThreshold, subjectPort, subjectId,
                this.subjectKeyManager, this.subjectSelf, this.executor, this.subjectSendQueuePool, this.subjectReceivedMailQueue, this.limiter,
                this.subjectConnectionPool, keyLifetime);
        this.executor.submit(instance);

        // 接続受け付け。
        final Socket testerSocket = this.testerServerSocket.accept();
        testerSocket.setSoTimeout((int) connectionTimeout);
        final InputStream testerInput = new BufferedInputStream(testerSocket.getInputStream());
        final OutputStream testerOutput = new BufferedOutputStream(testerSocket.getOutputStream());
        final Transceiver testerTransceiver = new Transceiver(transceiverShare, testerInput, testerOutput, null);

        // 一言目を受信。
        final FirstMessage message1 = (FirstMessage) StartingProtocol.receiveFirst(testerTransceiver);
        final PublicKey subjectPublicKey = message1.getKey();

        // 一言目への相槌を送信。
        final Key communicationKey = CryptographicKeys.newCommonKey();
        StartingProtocol.sendFirstReply(testerTransceiver, subjectPublicKey, communicationKey);

        // 二言目を受信。
        final SecondMessage message2 = StartingProtocol.receiveSecond(testerTransceiver, communicationKey);
        Assert.assertEquals(subjectId.getPublic(), message2.getId());
        Assert.assertArrayEquals(communicationKey.getEncoded(), CryptographicFunctions.decrypt(message2.getId(), message2.getEncryptedKey()));
        final byte[] watchword = message2.getWatchword();
        Assert.assertEquals(subjectPort, message2.getPort());
        Assert.assertEquals(connectionType, message2.getType());

        // 二言目への相槌を送信。
        final InetSocketAddress subject = new InetSocketAddress(testerSocket.getInetAddress(), subjectPort);
        StartingProtocol.sendSecondReply(testerTransceiver, communicationKey, testerId, watchword, testerPublicKeyPair.getPublic(),
                version + versionGapThreshold, subject);

        // 切断待ち。
        Assert.assertEquals(-1, testerInput.read());

        // 報告の確認。
        final NewProtocolWarning newProtocol = (NewProtocolWarning) this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertEquals(version, newProtocol.getVersion());
        Assert.assertEquals(version + 1, newProtocol.getNewVersion());
    }

}
