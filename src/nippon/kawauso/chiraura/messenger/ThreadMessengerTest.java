/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import nippon.kawauso.chiraura.lib.Duration;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class ThreadMessengerTest {

    private static final int port1 = 44444;
    private static final int port2 = port1 + 12345;
    private static final KeyPair id1 = CryptographicKeys.newPublicKeyPair();
    private static final KeyPair id2 = CryptographicKeys.newPublicKeyPair();
    private static final long connectionTimeout = 10 * Duration.SECOND;
    private static final long operationTimeout = Duration.SECOND;
    private static final int receiveBufferSize = 128 * 1024;
    private static final int sendBufferSize = 128 * 1024;
    private static final int messageSizeLimit = 1024 * 1024;
    private static final boolean http = false;
    private static final long publicKeyLifetime = Duration.DAY;
    private static final long commonKeyLifetime = Duration.DAY;
    private static final long version = 1L;
    private static final long versionGapThreshold = 1L;

    private static final boolean portIgnore = true;
    private static final int connectionLimit = 5;
    private static final long duration = Duration.SECOND / 2;
    private static final long sizeLimit = 10_000_000L;
    private static final int countLimit = 1_000;
    private static final long penalty = 5 * Duration.SECOND;

    private final ExecutorService executor;

    /**
     * 初期化
     */
    public ThreadMessengerTest() {
        this.executor = Executors.newCachedThreadPool();

    }

    /**
     * 正常系。
     * @throws Exception 異常
     */
    @Test
    public void testSample() throws Exception {
        // LoggingFunctions.startDebugLogging();

        final ThreadMessenger messenger1 = new ThreadMessenger(port1, receiveBufferSize, sendBufferSize, connectionTimeout, operationTimeout, messageSizeLimit,
                http, version, versionGapThreshold, id1, publicKeyLifetime, commonKeyLifetime, portIgnore, connectionLimit, duration, sizeLimit, countLimit,
                penalty);
        final ThreadMessenger messenger2 = new ThreadMessenger(port2, receiveBufferSize, sendBufferSize, connectionTimeout, operationTimeout, messageSizeLimit,
                http, version, versionGapThreshold, id2, publicKeyLifetime, commonKeyLifetime, portIgnore, connectionLimit, duration, sizeLimit, countLimit,
                penalty);

        messenger1.start(this.executor);
        messenger2.start(this.executor);

        // 接続受け付け開始待ち。
        Thread.sleep(100L);

        // 1 -> 2.
        final InetSocketAddress peer2 = new InetSocketAddress(InetAddress.getLocalHost(), port2);
        final int connectionType1_1 = ConnectionTypes.CONTROL;
        final List<Message> sendMail1_1 = new ArrayList<>();
        sendMail1_1.add(new TestMessage("いろはにへちま"));
        messenger1.send(peer2, connectionType1_1, sendMail1_1);

        final ReceivedMail recvMail2_1 = messenger2.take();
        Assert.assertEquals(sendMail1_1, recvMail2_1.getMail());
        Assert.assertEquals(id1.getPublic(), recvMail2_1.getSourceId());
        Assert.assertEquals(port1, recvMail2_1.getSourcePeer().getPort());
        Assert.assertEquals(connectionType1_1, recvMail2_1.getConnectionType());

        final SelfReport selfReport1 = (SelfReport) messenger1.takeReport();
        Assert.assertEquals(port1, selfReport1.getSelf().getPort());
        final ConnectReport connectReport1_1 = (ConnectReport) messenger1.takeReport();
        Assert.assertEquals(id2.getPublic(), connectReport1_1.getDestinationId());
        Assert.assertEquals(port2, connectReport1_1.getDestination().getPort());

        final SelfReport selfReport2 = (SelfReport) messenger2.takeReport();
        Assert.assertEquals(port2, selfReport2.getSelf().getPort());
        final ConnectReport connectReport2_1 = (ConnectReport) messenger2.takeReport();
        Assert.assertEquals(id1.getPublic(), connectReport2_1.getDestinationId());
        Assert.assertEquals(port1, connectReport2_1.getDestination().getPort());

        // 2 -> 1.
        final InetSocketAddress peer1 = recvMail2_1.getSourcePeer();
        final int connectionType2_1 = connectionType1_1 + 1;
        final List<Message> sendMail2_1 = new ArrayList<>();
        sendMail2_1.add(new TestMessage("いろはにへちま"));
        messenger2.send(peer1, connectionType2_1, sendMail2_1);

        final ReceivedMail recvMail1_1 = messenger1.take();
        Assert.assertEquals(sendMail2_1, recvMail1_1.getMail());
        Assert.assertEquals(id2.getPublic(), recvMail1_1.getSourceId());
        Assert.assertEquals(port2, recvMail1_1.getSourcePeer().getPort());
        Assert.assertEquals(connectionType2_1, recvMail1_1.getConnectionType());

        final ConnectReport connectReport1_2 = (ConnectReport) messenger1.takeReport();
        Assert.assertEquals(id2.getPublic(), connectReport1_2.getDestinationId());
        Assert.assertEquals(port2, connectReport1_2.getDestination().getPort());

        final SelfReport selfReport2_2 = (SelfReport) messenger2.takeReport();
        Assert.assertEquals(port2, selfReport2_2.getSelf().getPort());
        final ConnectReport connectReport2_2 = (ConnectReport) messenger2.takeReport();
        Assert.assertEquals(id1.getPublic(), connectReport2_2.getDestinationId());
        Assert.assertEquals(port1, connectReport2_2.getDestination().getPort());

        // 大容量送受信。
        final StringBuilder buff = new StringBuilder(1023 * 1024);
        final Random random = new Random();
        for (int i = 0; i < 1023 * 1024; i++) {
            buff.append((char) ('a' + random.nextInt(26)));
        }
        final List<Message> sendMail1_2 = new ArrayList<>();
        sendMail1_2.add(new TestMessage(buff.toString()));
        messenger1.send(peer2, connectionType1_1, sendMail1_2);

        final ReceivedMail recvMail2_2 = messenger2.take();
        Assert.assertEquals(sendMail1_2, recvMail2_2.getMail());
        Assert.assertEquals(id1.getPublic(), recvMail2_2.getSourceId());
        Assert.assertEquals(port1, recvMail2_2.getSourcePeer().getPort());
        Assert.assertEquals(connectionType1_1, recvMail2_2.getConnectionType());

        this.executor.shutdownNow();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));

        Assert.assertNull(messenger1.takeIfExists());
        Assert.assertNull(messenger1.takeReportIfExists());
        Assert.assertNull(messenger2.takeIfExists());
        Assert.assertNull(messenger2.takeReportIfExists());
    }

}
