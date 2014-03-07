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
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class BossTest {

    private static final int messageSizeLimit = 1024 * 1024;
    private static final TypeRegistry<Message> registry;
    static {
        registry = TypeRegistries.newRegistry();
        RegistryInitializer.init(registry);
    }
    private static final Transceiver transceiver = new Transceiver(messageSizeLimit, registry);

    /*
     * 検査インスタンス側は a、検査者側は b を頭に付ける。
     */
    private static final long connectionTimeout = 10 * Duration.SECOND;
    private static final long operationTimeout = Duration.SECOND;
    private static final int receiveBufferSize = 128 * 1024;
    private static final int sendBufferSize = 128 * 1024;
    private static final long publicKeyLifetime = 100 * Duration.SECOND;
    private static final long commonKeyLifetime = 10 * Duration.SECOND;
    private static final int connectionType = ConnectionTypes.DATA;
    private static final long version = 1L;
    private static final long versionGapThreshold = 1L;

    private static final int subjectPort = 12345;
    private static final KeyPair subjectId = CryptographicKeys.newPublicKeyPair();
    private static final KeyPair testerPublicKeyPair = CryptographicKeys.newPublicKeyPair();

    private static final int testerPort = subjectPort + 10;
    private static final KeyPair testerId = CryptographicKeys.newPublicKeyPair();

    private static final boolean portIgnore = false;
    private static final int connectionLimit = 5;

    private static final long duration = Duration.SECOND / 2;
    private static final long sizeLimit = 10_000_000L;
    private static final int countLimit = 1_000;
    private static final long penalty = 5 * Duration.SECOND;

    private final ExecutorService executor;

    private final BlockingQueue<ReceivedMail> subjectReceivedMailQueue;
    private final SendQueuePool subjectSendQueuePool;
    private final BlockingQueue<ConnectRequest> subjectConnectRequestQueue;
    private final BlockingQueue<MessengerReport> subjectMessengerReportQueue;
    private final ConnectionPool<AcceptedConnection> subjectAcceptedConnectionPool;
    private final ConnectionPool<ContactingConnection> subjectContactingConnectionPool;
    private final ConnectionPool<Connection> subjectConnectionPool;

    private final AtomicReference<InetSocketAddress> subjectSelf;

    private final ServerSocket testerServerSocket;

    /**
     * 初期化。
     * @throws Exception 異常
     */
    public BossTest() throws Exception {
        this.executor = Executors.newCachedThreadPool();

        this.subjectReceivedMailQueue = new LinkedBlockingQueue<>();
        this.subjectSendQueuePool = new BasicSendQueuePool();
        this.subjectConnectRequestQueue = new LinkedBlockingQueue<>();
        this.subjectMessengerReportQueue = new LinkedBlockingQueue<>();
        this.subjectAcceptedConnectionPool = new PortIgnoringConnectionPool<>();
        this.subjectContactingConnectionPool = new BoundConnectionPool<>();
        this.subjectConnectionPool = new BoundConnectionPool<>();

        this.subjectSelf = new AtomicReference<>(null);

        this.testerServerSocket = new ServerSocket();
        this.testerServerSocket.setReuseAddress(true);
        this.testerServerSocket.bind(new InetSocketAddress(testerPort));
    }

    /**
     * 終処理。
     * @throws Exception 異常
     */
    @After
    public void tearDown() throws Exception {
        this.executor.shutdownNow();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        Assert.assertNull(this.subjectReceivedMailQueue.poll());
        Assert.assertNull(this.subjectMessengerReportQueue.poll());
    }

    /**
     * 正常系。
     * @throws Exception エラー
     */
    @Test
    public void testSample() throws Exception {
        final Boss instance = new Boss(this.executor, this.subjectConnectRequestQueue, this.subjectReceivedMailQueue, this.subjectSendQueuePool,
                this.subjectMessengerReportQueue, this.subjectAcceptedConnectionPool, this.subjectContactingConnectionPool, this.subjectConnectionPool,
                subjectPort, receiveBufferSize, sendBufferSize, connectionTimeout, operationTimeout, messageSizeLimit, registry, version, versionGapThreshold,
                subjectId, publicKeyLifetime, commonKeyLifetime, this.subjectSelf, portIgnore, connectionLimit, duration, sizeLimit, countLimit, penalty);
        this.executor.submit(instance);

        contactorTest(null); // ループバック。
        contactorTest(new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 169, (byte) 254, 0, 1 }), subjectPort)); // リンクローカル。
        contactorTest(new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192, (byte) 168, 0, 1 }), subjectPort)); // サイトローカル。
        contactorTest(new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 218, (byte) 221, 59, (byte) 241 }), subjectPort)); // グローバル。
        contactorTest(new InetSocketAddress(InetAddress.getByAddress(new byte[] { 74, 125, (byte) 235, (byte) 152 }), subjectPort)); // グローバル。

        try (final Socket testerSocket = new Socket(InetAddress.getLocalHost(), subjectPort)) {
            // Acceptor 側の検査。
            final InputStream testerInput = new BufferedInputStream(testerSocket.getInputStream());
            final OutputStream testerOutput = new BufferedOutputStream(testerSocket.getOutputStream());

            // 一言目の送信。
            StartingProtocol.sendFirst(transceiver, testerOutput, testerPublicKeyPair.getPublic());

            // 一言目への相槌を受信。
            final FirstReply reply1 = StartingProtocol.receiveFirstReply(transceiver, testerInput, testerPublicKeyPair.getPrivate());
            final Key communicationKey = reply1.getKey();

            // 二言目の送信。
            final Random random = new Random();
            final byte[] watchword = new byte[CryptographicKeys.PUBLIC_KEY_SIZE / Byte.SIZE / 2];
            random.nextBytes(watchword);
            StartingProtocol.sendSecond(transceiver, testerOutput, testerId, communicationKey, watchword, version, testerPort, connectionType,
                    (InetSocketAddress) testerSocket.getRemoteSocketAddress());

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
            final SecondReply reply2 = (SecondReply) StartingProtocol.receiveSecondReply(transceiver, testerInput, communicationKey);

            Assert.assertEquals(subjectId.getPublic(), reply2.getId());
            Assert.assertArrayEquals(watchword, CryptographicFunctions.decrypt(reply2.getId(), reply2.getEncryptedWatchword()));

            // 報告の確認。
            // グローバル IP で設定されているので SelfReport は来ない。
            final ConnectReport connectReport = (ConnectReport) this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
            Assert.assertEquals(testerId.getPublic(), connectReport.getDestinationId());
            Assert.assertEquals(testerPort, connectReport.getDestination().getPort());

            // 検査インスタンス側 Sender の稼動確認。
            final List<Message> sendMail = new ArrayList<>(1);
            sendMail.add(new TestMessage("あほやで"));
            this.subjectSendQueuePool.put(connectReport.getDestination(), connectionType, sendMail);

            final List<Message> recvMail = new ArrayList<>(1);
            transceiver.fromStream(testerInput, communicationKey, recvMail);
            Assert.assertEquals(sendMail, recvMail);

            // 検査インスタンス側 Receiver の稼動確認。
            sendMail.clear();
            sendMail.add(new TestMessage("あほかね"));
            transceiver.toStream(testerOutput, sendMail, EncryptedEnvelope.class, communicationKey);
            testerOutput.flush();

            final ReceivedMail receivedMail = this.subjectReceivedMailQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
            Assert.assertEquals(sendMail, receivedMail.getMail());

            // 接続が登録されているかどうか。
            Assert.assertEquals(1, this.subjectConnectionPool.get(receivedMail.getSourcePeer()).size());
            Assert.assertEquals(connectionType, this.subjectConnectionPool.get(receivedMail.getSourcePeer()).get(0).getType());
        }
    }

    private void contactorTest(final InetSocketAddress subject) throws InterruptedException, IOException, MyRuleException {
        final InetSocketAddress tester = new InetSocketAddress(InetAddress.getLocalHost(), testerPort);

        this.subjectConnectRequestQueue.put(new ConnectRequest(tester, connectionType));

        // 接続受け付け。
        try (final Socket testerSocket = this.testerServerSocket.accept()) {
            testerSocket.setSoTimeout((int) connectionTimeout);
            final InputStream testerInput = new BufferedInputStream(testerSocket.getInputStream());
            final OutputStream testerOutput = new BufferedOutputStream(testerSocket.getOutputStream());

            // 一言目を受信。
            final FirstMessage message1 = (FirstMessage) StartingProtocol.receiveFirst(transceiver, testerInput);
            final PublicKey subjectPublicKey = message1.getKey();

            // 一言目への相槌を送信。
            final Key communicationKey = CryptographicKeys.newCommonKey();
            StartingProtocol.sendFirstReply(transceiver, testerOutput, subjectPublicKey, communicationKey);

            // 二言目を受信。
            final SecondMessage message2 = StartingProtocol.receiveSecond(transceiver, testerInput, communicationKey);
            Assert.assertEquals(subjectId.getPublic(), message2.getId());
            Assert.assertArrayEquals(communicationKey.getEncoded(), CryptographicFunctions.decrypt(message2.getId(), message2.getEncryptedKey()));
            final byte[] watchword = message2.getWatchword();
            Assert.assertEquals(subjectPort, message2.getPort());
            Assert.assertEquals(connectionType, message2.getType());

            // 二言目への相槌を送信。
            final InetSocketAddress declaredSubject = (subject != null ? subject : new InetSocketAddress(testerSocket.getInetAddress(), subjectPort));
            StartingProtocol.sendSecondReply(transceiver, testerOutput, communicationKey, testerId, watchword, testerPublicKeyPair.getPublic(), version,
                    declaredSubject);

            // 報告の確認。
            final SelfReport selfReport = (SelfReport) this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
            Assert.assertEquals(declaredSubject, selfReport.getSelf());
            final ConnectReport connectReport = (ConnectReport) this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
            Assert.assertEquals(testerId.getPublic(), connectReport.getDestinationId());
            Assert.assertEquals(testerPort, connectReport.getDestination().getPort());

            // 検査インスタンス側 Sender の稼動確認。
            final List<Message> sendMail = new ArrayList<>(1);
            sendMail.add(new TestMessage("あほやで"));
            this.subjectSendQueuePool.put(message2.getPeer(), connectionType, sendMail);

            final List<Message> recvMail = new ArrayList<>(1);
            transceiver.fromStream(testerInput, communicationKey, recvMail);
            Assert.assertEquals(sendMail, recvMail);

            // 検査インスタンス側 Receiver の稼動確認。
            sendMail.clear();
            sendMail.add(new TestMessage("あほかね"));
            transceiver.toStream(testerOutput, sendMail, EncryptedEnvelope.class, communicationKey);
            testerOutput.flush();

            final ReceivedMail receivedMail = this.subjectReceivedMailQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
            Assert.assertEquals(sendMail, receivedMail.getMail());

            // 接続が登録されているかどうか。
            Assert.assertEquals(1, this.subjectConnectionPool.get(receivedMail.getSourcePeer()).size());
            Assert.assertEquals(connectionType, this.subjectConnectionPool.get(receivedMail.getSourcePeer()).get(0).getType());
        }
    }

}
