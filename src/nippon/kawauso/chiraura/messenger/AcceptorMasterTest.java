/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.process.Reporter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class AcceptorMasterTest {

    private static final Transceiver transceiver;
    static {
        final TypeRegistry<Message> registry = TypeRegistries.newRegistry();
        transceiver = new Transceiver(Integer.MAX_VALUE, RegistryInitializer.init(registry));
    }
    private static final boolean portIgnore = false;
    private static final int connectionLimit = 5;
    private static final long duration = Duration.SECOND / 2;
    private static final long sizeLimit = 10_000_000L;
    private static final int countLimit = 1_000;
    private static final long penalty = 5 * Duration.SECOND;
    private final TrafficLimiter limiter = new BasicConstantTrafficLimiter(duration, sizeLimit, countLimit, penalty);

    /*
     * 検査インスタンス側は a、検査者側は b を頭に付ける。
     */
    private static final int sendBufferSize = 128 * 1024;
    private static final long connectionTimeout = 10 * Duration.SECOND;
    private static final long operationTimeout = 10 * Duration.SECOND;
    private static final long keyLifetime = 10 * Duration.SECOND;
    private static final long version = 1L;
    private static final long versionGapThreshold = 1L;
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
    private final Socket subjectSocket;

    private final BlockingQueue<ReceivedMail> subjectReceivedMailQueue;
    private final SendQueuePool subjectSendQueuePool;
    private final BlockingQueue<MessengerReport> subjectMessengerReportQueue;
    private final ConnectionPool<AcceptedConnection> subjectAcceptedConnectionPool;
    private final ConnectionPool<Connection> subjectConnectionPool;

    private final AtomicInteger subjectSerialGenerator;
    private final BlockingQueue<Socket> subjectAcceptedSocketQueue;
    private final BlockingQueue<Reporter.Report> subjectReportQueue;

    private final PublicKeyManager subjectKeyManager;

    private final AtomicReference<InetSocketAddress> subjectSelf;

    /**
     * 初期化。
     * @throws Exception 異常
     */
    public AcceptorMasterTest() throws Exception {
        this.executor = Executors.newCachedThreadPool();

        this.testerServerSocket = new ServerSocket();
        this.testerServerSocket.setReuseAddress(true);
        this.testerServerSocket.bind(new InetSocketAddress(testerPort));
        this.subjectServerSocket = new ServerSocket();
        this.subjectServerSocket.setReuseAddress(true);
        this.subjectServerSocket.bind(new InetSocketAddress(testerPort + 10));
        this.testerSocket = new Socket(InetAddress.getLocalHost(), this.subjectServerSocket.getLocalPort());
        this.subjectSocket = this.subjectServerSocket.accept();
        this.testerInput = new BufferedInputStream(this.testerSocket.getInputStream());
        this.testerOutput = new BufferedOutputStream(this.testerSocket.getOutputStream());

        this.subjectReceivedMailQueue = new LinkedBlockingQueue<>();
        this.subjectSendQueuePool = new BasicSendQueuePool();
        this.subjectMessengerReportQueue = new LinkedBlockingQueue<>();
        this.subjectAcceptedConnectionPool = new PortIgnoringConnectionPool<>();
        this.subjectConnectionPool = new BoundConnectionPool<>();

        this.subjectSerialGenerator = new AtomicInteger();
        this.subjectAcceptedSocketQueue = new LinkedBlockingQueue<>();
        this.subjectReportQueue = new LinkedBlockingQueue<>();

        this.subjectKeyManager = new PublicKeyManager(100 * Duration.SECOND);

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
        this.subjectSocket.close();
        this.testerInput.close();
        this.testerOutput.close();
        this.testerSocket.close();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        Assert.assertEquals(new ArrayList<AcceptedConnection>(0), this.subjectAcceptedConnectionPool.getAll());
        Assert.assertNull(this.subjectMessengerReportQueue.poll());
        Assert.assertNull(this.subjectReportQueue.poll());
    }

    /**
     * 正常系。
     * @throws Exception エラー
     */
    @Test
    public void testSample() throws Exception {
        final AcceptorMaster instance = new AcceptorMaster(this.subjectReportQueue, this.subjectAcceptedSocketQueue, this.subjectSerialGenerator,
                this.executor, portIgnore, connectionLimit, this.subjectReceivedMailQueue, this.subjectSendQueuePool, this.limiter,
                this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool, this.subjectConnectionPool, sendBufferSize, connectionTimeout,
                operationTimeout, transceiver, version, versionGapThreshold, subjectId, this.subjectKeyManager, keyLifetime, this.subjectSelf);
        this.executor.submit(instance);

        this.subjectAcceptedSocketQueue.put(this.subjectSocket);

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
        final SelfReport selfReport = (SelfReport) this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertEquals(this.testerSocket.getRemoteSocketAddress(), selfReport.get());
        final ConnectReport connectReport = (ConnectReport) this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertEquals(testerId.getPublic(), connectReport.getDestinationId());
        Assert.assertEquals(testerPort, connectReport.getDestination().getPort());

        // 検査インスタンス側 Sender の稼動確認。
        final List<Message> sendMail = new ArrayList<>(1);
        sendMail.add(new TestMessage("あほやで"));
        this.subjectSendQueuePool.put(connectReport.getDestination(), connectionType, sendMail);

        final List<Message> recvMail = new ArrayList<>(1);
        transceiver.fromStream(this.testerInput, communicationKey, recvMail);
        Assert.assertEquals(sendMail, recvMail);

        // 検査インスタンス側 Receiver の稼動確認。
        sendMail.clear();
        sendMail.add(new TestMessage("あほかね"));
        transceiver.toStream(this.testerOutput, sendMail, EncryptedEnvelope.class, communicationKey);
        this.testerOutput.flush();

        final ReceivedMail receivedMail = this.subjectReceivedMailQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertEquals(sendMail, receivedMail.getMail());

        // 接続が登録されているかどうか。
        Assert.assertEquals(1, this.subjectConnectionPool.get(receivedMail.getSourcePeer()).size());
        Assert.assertEquals(connectionType, this.subjectConnectionPool.get(receivedMail.getSourcePeer()).get(0).getType());
    }

}
