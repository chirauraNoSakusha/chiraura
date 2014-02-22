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
import java.net.ServerSocket;
import java.net.Socket;
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

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.test.InputStreamWrapper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Receiver のみを動かす検査。
 * @author chirauraNoSakusha
 */
public final class ReceiverTest {

    private static final Transceiver transceiver;
    static {
        final TypeRegistry<Message> registry = TypeRegistries.newRegistry();
        transceiver = new Transceiver(Integer.MAX_VALUE, RegistryInitializer.init(registry));
    }
    private static final long duration = Duration.SECOND / 2;
    private static final long sizeLimit = 10_000_000L;
    private static final int countLimit = 1_000;
    private static final long penalty = 5 * Duration.SECOND;
    private final TrafficLimiter limiter = new ConstantTrafficLimiter(duration, sizeLimit, countLimit, penalty);

    private static final long timeout = 10 * Duration.SECOND;
    private static final int idNumber = 1234;
    private static final int connectionType = ConnectionTypes.DEFAULT;
    private static final Key commonKey = CryptographicKeys.newCommonKey();

    private static final InetSocketAddress tester = new InetSocketAddress("localhost", 9999);
    private static final PublicKey testerId = CryptographicKeys.newPublicKeyPair().getPublic();
    private static final KeyPair testerKeyPair = CryptographicKeys.newPublicKeyPair();

    private static final KeyPair subjectKeyPair = CryptographicKeys.newPublicKeyPair();

    private final ExecutorService executor;

    private final ServerSocket testerServerSocket;
    private final Socket testerSocket;
    private final OutputStream testerOutput;

    private final Socket subjectSocket;
    private final InputStream subjectInput;
    private final Connection subjectConnection;

    private final BlockingQueue<ReceivedMail> subjectReceivedMailQueue;
    private final BlockingQueue<MessengerReport> subjectMessengerReportQueue;

    /**
     * 初期化。
     * @throws Exception 異常
     */
    public ReceiverTest() throws Exception {
        this.executor = Executors.newCachedThreadPool();

        final int port = 12345;
        this.testerServerSocket = new ServerSocket(port);
        this.subjectSocket = new Socket("localhost", port);
        this.testerSocket = this.testerServerSocket.accept();
        this.testerOutput = new BufferedOutputStream(this.testerSocket.getOutputStream());
        this.testerSocket.shutdownInput();
        this.subjectInput = new BufferedInputStream(this.subjectSocket.getInputStream());
        this.subjectSocket.shutdownOutput();
        this.subjectConnection = new Connection(idNumber, tester, testerId, connectionType, this.subjectSocket);
        this.subjectReceivedMailQueue = new LinkedBlockingQueue<>();
        this.subjectMessengerReportQueue = new LinkedBlockingQueue<>();

        this.subjectSocket.setSoTimeout((int) timeout);

        // LoggingFunctions.startDebugLogging();
    }

    /**
     * 終処理。
     * @throws Exception 異常
     */
    @After
    public void tearDown() throws Exception {
        this.testerOutput.close();
        this.testerSocket.close();
        this.testerServerSocket.close();
        this.subjectConnection.close();
        this.subjectInput.close();
        this.subjectSocket.close();
        this.executor.shutdownNow();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        Assert.assertTrue(this.subjectReceivedMailQueue.isEmpty());
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
        final Receiver instance = new Receiver(this.subjectReceivedMailQueue, this.subjectMessengerReportQueue, this.limiter, timeout, transceiver,
                this.subjectConnection, this.subjectInput, subjectKeyPair.getPrivate(), testerKeyPair.getPublic(), commonKey);
        this.executor.submit(instance);

        // 一回目。
        final List<Message> sendMail1 = new ArrayList<>(1);
        sendMail1.add(new TestMessage("あほやで"));
        transceiver.toStream(this.testerOutput, sendMail1, PlainEnvelope.class, commonKey);
        this.testerOutput.flush();

        final ReceivedMail recvMail1 = this.subjectReceivedMailQueue.take();
        Assert.assertEquals(tester, recvMail1.getSourcePeer());
        Assert.assertEquals(connectionType, recvMail1.getConnectionType());
        Assert.assertEquals(sendMail1, recvMail1.getMail());

        // 二回目。
        final List<Message> sendMail2 = new ArrayList<>(1);
        sendMail2.add(new TestMessage(129876));
        transceiver.toStream(this.testerOutput, sendMail2, EncryptedEnvelope.class, commonKey);
        this.testerOutput.flush();

        final ReceivedMail recvMail2 = this.subjectReceivedMailQueue.take();
        Assert.assertEquals(tester, recvMail2.getSourcePeer());
        Assert.assertEquals(connectionType, recvMail2.getConnectionType());
        Assert.assertEquals(sendMail2, recvMail2.getMail());
    }

    /**
     * 鍵更新の検査。
     * @throws Exception エラー
     */
    @Test
    public void testKeyUpdate() throws Exception {
        final Receiver instance = new Receiver(this.subjectReceivedMailQueue, this.subjectMessengerReportQueue, this.limiter, timeout, transceiver,
                this.subjectConnection, this.subjectInput, subjectKeyPair.getPrivate(), testerKeyPair.getPublic(), commonKey);
        this.executor.submit(instance);

        // 一回目。
        final List<Message> sendMail1 = new ArrayList<>(2);
        sendMail1.add(new TestMessage("あほやで"));
        final Key newCommonKey = CryptographicKeys.newCommonKey();
        sendMail1.add(KeyUpdateMessage.newInstance(subjectKeyPair.getPublic(), testerKeyPair.getPrivate(), newCommonKey));
        transceiver.toStream(this.testerOutput, sendMail1, EncryptedEnvelope.class, commonKey);
        this.testerOutput.flush();

        final ReceivedMail recvMail1 = this.subjectReceivedMailQueue.take();
        Assert.assertEquals(tester, recvMail1.getSourcePeer());
        Assert.assertEquals(connectionType, recvMail1.getConnectionType());
        Assert.assertEquals(sendMail1.get(0), recvMail1.getMail().get(0));

        // 二回目。
        final List<Message> sendMail2 = new ArrayList<>(1);
        sendMail2.add(new TestMessage(129876));
        transceiver.toStream(this.testerOutput, sendMail2, EncryptedEnvelope.class, newCommonKey);
        this.testerOutput.flush();

        final ReceivedMail recvMail2 = this.subjectReceivedMailQueue.take();
        Assert.assertEquals(tester, recvMail2.getSourcePeer());
        Assert.assertEquals(connectionType, recvMail2.getConnectionType());
        Assert.assertEquals(sendMail2, recvMail2.getMail());
    }

    /**
     * 通信異常。
     * @throws Exception エラー
     */
    @Test
    public void testErrorStream() throws Exception {
        // LoggingFunctions.startDebugLogging();

        final int errorThreshold = 5;
        final InputStream subjectErrorInput = InputStreamWrapper.getErrorStream(this.subjectInput, errorThreshold);

        final Receiver instance = new Receiver(this.subjectReceivedMailQueue, this.subjectMessengerReportQueue, this.limiter, errorThreshold, transceiver,
                this.subjectConnection, subjectErrorInput, subjectKeyPair.getPrivate(), testerKeyPair.getPublic(), commonKey);
        final Future<Void> future = this.executor.submit(instance);

        for (int i = 0; i < errorThreshold; i++) {
            final List<Message> mail = new ArrayList<>(1);
            mail.add(new TestMessage(129876));
            try {
                transceiver.toStream(this.testerOutput, mail, EncryptedEnvelope.class, commonKey);
                this.testerOutput.flush();
            } catch (final IOException e) {
                break;
            }

            final ReceivedMail receivedMail = this.subjectReceivedMailQueue.poll(100L, TimeUnit.MILLISECONDS);
            if (receivedMail == null) {
                break;
            }
            Assert.assertEquals(tester, receivedMail.getSourcePeer());
            Assert.assertEquals(connectionType, receivedMail.getConnectionType());
            Assert.assertEquals(mail, receivedMail.getMail());

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
    public void testInvalidMail() throws Exception {
        final Receiver instance = new Receiver(this.subjectReceivedMailQueue, this.subjectMessengerReportQueue, this.limiter, timeout, transceiver,
                this.subjectConnection, this.subjectInput, subjectKeyPair.getPrivate(), testerKeyPair.getPublic(), commonKey);
        final Future<Void> future = this.executor.submit(instance);

        this.testerOutput.write(new byte[100]);
        this.testerOutput.flush();

        future.get(Duration.SECOND, TimeUnit.MILLISECONDS);

        final MessengerReport report = this.subjectMessengerReportQueue.poll(Duration.SECOND, TimeUnit.MILLISECONDS);
        Assert.assertTrue(report instanceof CommunicationError);
        Assert.assertTrue(((CommunicationError) report).getError() instanceof MyRuleException);
    }

    /**
     * 時間切れ。
     * @throws Exception エラー
     */
    @Test
    public void testTimeout() throws Exception {
        final long shortTimeout = 100L;
        this.subjectSocket.setSoTimeout((int) shortTimeout);
        final Receiver instance = new Receiver(this.subjectReceivedMailQueue, this.subjectMessengerReportQueue, this.limiter, shortTimeout, transceiver,
                this.subjectConnection, this.subjectInput, subjectKeyPair.getPrivate(), testerKeyPair.getPublic(), commonKey);
        final Future<Void> future = this.executor.submit(instance);

        // 時間切れ待ち。
        future.get(shortTimeout + Duration.SECOND, TimeUnit.MILLISECONDS);
    }

}
