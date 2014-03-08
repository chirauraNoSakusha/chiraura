/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Sender のみを動かす検査。
 * @author chirauraNoSakusha
 */
public final class SenderTest {

    private static final TransceiverShare transceiverShare;
    static {
        final TypeRegistry<Message> registry = TypeRegistries.newRegistry();
        transceiverShare = new TransceiverShare(Integer.MAX_VALUE, RegistryInitializer.init(registry));
    }

    private static final long timeout = 10 * Duration.SECOND;
    private static final int idNumber = 1234;
    private static final int connectionType = ConnectionTypes.DEFAULT;
    private static final long keyLifetime = 10 * Duration.SECOND;
    private static final Key commonKey = CryptographicKeys.newCommonKey();

    private static final InetSocketAddress tester = new InetSocketAddress("localhost", 9999);
    private static final KeyPair testerIdPair = CryptographicKeys.newPublicKeyPair();
    private static final KeyPair testerKeyPair = CryptographicKeys.newPublicKeyPair();

    private static final KeyPair subjectKeyPair = CryptographicKeys.newPublicKeyPair();

    private final ExecutorService executor;

    private final Socket testerSocket;
    private final InputStream testerInput;

    private final ServerSocket subjectServerSocket;
    private final Socket subjectSocket;
    private final OutputStream subjectOutput;
    private final Connection subjectConnection;
    private final SendQueuePool subjectSendQueuePool;
    private final BlockingQueue<MessengerReport> subjectMessengerReportQueue;
    private final ConnectionPool<Connection> subjectConnectionPool;

    /**
     * 初期化。
     * @throws Exception 異常
     */
    public SenderTest() throws Exception {
        this.executor = Executors.newCachedThreadPool();

        final int port = 12345;
        this.subjectServerSocket = new ServerSocket();
        this.subjectServerSocket.setReuseAddress(true);
        this.subjectServerSocket.bind(new InetSocketAddress(port));
        this.testerSocket = new Socket("localhost", port);
        this.subjectSocket = this.subjectServerSocket.accept();
        this.testerInput = new BufferedInputStream(this.testerSocket.getInputStream());
        this.testerSocket.shutdownOutput();
        this.subjectOutput = new BufferedOutputStream(this.subjectSocket.getOutputStream());
        this.subjectSocket.shutdownInput();
        this.subjectConnection = new Connection(idNumber, tester, testerIdPair.getPublic(), connectionType, this.subjectSocket);
        this.subjectSendQueuePool = new BasicSendQueuePool();
        this.subjectMessengerReportQueue = new LinkedBlockingQueue<>();
        this.subjectConnectionPool = new BoundConnectionPool<>();

        this.subjectConnectionPool.add(this.subjectConnection);

        // DebugFunctions.startDebugLogging();
    }

    /**
     * 終処理。
     * @throws Exception 異常
     */
    @After
    public void tearDown() throws Exception {
        this.testerInput.close();
        this.testerSocket.close();
        this.subjectConnection.close();
        this.subjectOutput.close();
        this.subjectSocket.close();
        this.subjectServerSocket.close();
        this.executor.shutdownNow();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        Assert.assertEquals(new ArrayList<Connection>(0), this.subjectConnectionPool.getAll());
        Assert.assertFalse(this.subjectSendQueuePool.containsQueue(tester, connectionType));
        Assert.assertNull(this.subjectMessengerReportQueue.poll());
    }

    /**
     * 正常系。
     * @throws Exception エラー
     */
    @Test
    public void testSample() throws Exception {
        final Transceiver subjectTransceiver = new Transceiver(transceiverShare, new ByteArrayInputStream(new byte[0]), this.subjectOutput);
        final Transceiver testerTransceiver = new Transceiver(transceiverShare, this.testerInput, new ByteArrayOutputStream());
        final Sender instance = new Sender(this.subjectSendQueuePool, this.subjectMessengerReportQueue, this.subjectConnectionPool, timeout,
                subjectTransceiver, this.subjectConnection, keyLifetime, subjectKeyPair.getPrivate(), testerKeyPair.getPublic(), commonKey);
        this.executor.submit(instance);

        // キューの登録待ち。
        Thread.sleep(100L);

        // 一回目。
        final List<Message> sendMail1 = new ArrayList<>(1);
        sendMail1.add(new TestMessage("あほやで"));
        this.subjectSendQueuePool.put(tester, connectionType, sendMail1);

        final List<Message> recvMail1 = new ArrayList<>(1);
        testerTransceiver.fromStream(commonKey, recvMail1);
        Assert.assertEquals(sendMail1, recvMail1);

        // 二回目。
        final List<Message> sendMail2 = new ArrayList<>(1);
        sendMail2.add(new TestMessage(123456));
        this.subjectSendQueuePool.put(tester, connectionType, sendMail2);

        final List<Message> recvMail2 = new ArrayList<>(1);
        testerTransceiver.fromStream(commonKey, recvMail2);
        Assert.assertEquals(sendMail2, recvMail2);
    }

    /**
     * 鍵更新の検査。
     * @throws Exception エラー
     */
    @Test
    public void testKeyUpdate() throws Exception {
        final Transceiver subjectTransceiver = new Transceiver(transceiverShare, new ByteArrayInputStream(new byte[0]), this.subjectOutput);
        final Transceiver testerTransceiver = new Transceiver(transceiverShare, this.testerInput, new ByteArrayOutputStream());
        final long shortKeyLifetime = 200L;
        final Sender instance = new Sender(this.subjectSendQueuePool, this.subjectMessengerReportQueue, this.subjectConnectionPool, timeout,
                subjectTransceiver, this.subjectConnection, shortKeyLifetime, subjectKeyPair.getPrivate(), testerKeyPair.getPublic(), commonKey);
        this.executor.submit(instance);

        // 鍵の期限切れ待ち。
        Thread.sleep(100L + shortKeyLifetime);

        // 一回目。
        final List<Message> sendMail1 = new ArrayList<>(2);
        sendMail1.add(new TestMessage("あほやで"));
        this.subjectSendQueuePool.put(tester, connectionType, sendMail1);

        final List<Message> recvMail1 = new ArrayList<>(2);
        testerTransceiver.fromStream(commonKey, recvMail1);
        Assert.assertEquals(2, recvMail1.size());
        Assert.assertTrue(recvMail1.get(1) instanceof KeyUpdateMessage);
        Assert.assertEquals(sendMail1.get(0), recvMail1.get(0));

        final Key newCommonKey = ((KeyUpdateMessage) recvMail1.get(1)).getKey(testerKeyPair.getPrivate(), subjectKeyPair.getPublic());

        // 二回目。
        final List<Message> sendMail2 = new ArrayList<>(1);
        sendMail2.add(new TestMessage(123456));
        this.subjectSendQueuePool.put(tester, connectionType, sendMail2);

        final List<Message> recvMail2 = new ArrayList<>(1);
        testerTransceiver.fromStream(newCommonKey, recvMail2);
        Assert.assertEquals(sendMail2, recvMail2);
    }

    /**
     * 通信異常。
     * @throws Exception エラー
     */
    @Test
    public void testErrorStream() throws Exception {
        final Transceiver subjectTransceiver = new Transceiver(transceiverShare, new ByteArrayInputStream(new byte[0]), this.subjectOutput);
        final Sender instance = new Sender(this.subjectSendQueuePool, this.subjectMessengerReportQueue, this.subjectConnectionPool, timeout,
                subjectTransceiver, this.subjectConnection, keyLifetime, subjectKeyPair.getPrivate(), testerKeyPair.getPublic(), commonKey);
        final Future<Void> future = this.executor.submit(instance);

        this.testerSocket.close();

        // キューの登録待ち。
        Thread.sleep(100L);

        while (this.subjectSendQueuePool.size(tester, connectionType) == 0) {
            final List<Message> mail = new ArrayList<>(1);
            mail.add(new TestMessage("あほやで"));
            this.subjectSendQueuePool.put(tester, connectionType, mail);

            // 動作待ち。
            Thread.sleep(100L);
        }

        future.get(Duration.SECOND, TimeUnit.MILLISECONDS);

        final MessengerReport report = this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertTrue(report instanceof CommunicationError);
        Assert.assertTrue(((CommunicationError) report).getError() instanceof IOException);
    }

    /**
     * メッセージ規約違反。
     * @throws Exception エラー
     */
    @Test
    public void testInvalidMessage() throws Exception {
        final Transceiver subjectTransceiver = new Transceiver(transceiverShare, new ByteArrayInputStream(new byte[0]), this.subjectOutput);
        final Sender instance = new Sender(this.subjectSendQueuePool, this.subjectMessengerReportQueue, this.subjectConnectionPool, timeout,
                subjectTransceiver, this.subjectConnection, keyLifetime, subjectKeyPair.getPrivate(), testerKeyPair.getPublic(), commonKey);
        final Future<Void> future = this.executor.submit(instance);

        // キューの登録待ち。
        Thread.sleep(100L);

        final List<Message> mail = new ArrayList<>();
        mail.add(new Message() {
            @Override
            public int byteSize() {
                return 1;
            }

            @Override
            public int toStream(final OutputStream output1) throws IOException {
                output1.write(0);
                return 1;
            }
        });
        this.subjectSendQueuePool.put(tester, connectionType, mail);

        future.get(Duration.SECOND, TimeUnit.MILLISECONDS);

        final MessengerReport report = this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertTrue(report instanceof CommunicationError);
        Assert.assertTrue(((CommunicationError) report).getError() instanceof IllegalStateException);
    }

    /**
     * 時間切れ。
     * @throws Exception エラー
     */
    @Test
    public void testTimeout() throws Exception {
        final Transceiver subjectTransceiver = new Transceiver(transceiverShare, new ByteArrayInputStream(new byte[0]), this.subjectOutput);
        final long shortTimeout = 100L;
        final Sender instance = new Sender(this.subjectSendQueuePool, this.subjectMessengerReportQueue, this.subjectConnectionPool, shortTimeout,
                subjectTransceiver, this.subjectConnection, shortTimeout, subjectKeyPair.getPrivate(), testerKeyPair.getPublic(), commonKey);
        final Future<Void> future = this.executor.submit(instance);

        // 時間切れ待ち。
        future.get(shortTimeout + Duration.SECOND, TimeUnit.MILLISECONDS);
    }

}
