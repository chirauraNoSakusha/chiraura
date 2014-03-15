package nippon.kawauso.chiraura.closet.p2p;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.process.Reporter;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class NetworkManagerTest {

    private static final long timeout = 15 * Duration.SECOND;
    private static final long shutdownTimeout = Duration.SECOND;

    private final Random random;
    private final BlockingQueue<Reporter.Report> reportQueue;
    private final NetworkWrapper network;
    final PeerAccessNonBlockingDriver peerDriver;
    final AddressAccessNonBlockingDriver addressDriver;
    private final ExecutorService executor;

    /**
     * 初期化。
     */
    public NetworkManagerTest() {
        this.random = new Random();
        this.reportQueue = new LinkedBlockingQueue<>();
        this.network = NetworkWrapperTest.sample(this.random, new HashingCalculator(1_000));
        this.executor = Executors.newCachedThreadPool();
        final SessionManager sessionManager = new SessionManager();
        final PeerAccessDriver peerRemoteDriver = new PeerAccessDriver(sessionManager, this.network);
        this.peerDriver = new PeerAccessNonBlockingDriver(new OperationAggregator<PeerAccessOperation, PeerAccessResult>(), peerRemoteDriver, this.executor);
        final AddressAccessDriver addressRemoteDriver = new AddressAccessDriver(sessionManager, this.network);
        this.addressDriver = new AddressAccessNonBlockingDriver(new OperationAggregator<AddressAccessOperation, AddressAccessResult>(), addressRemoteDriver,
                this.executor);
    }

    /**
     * 起動試験。
     * @throws Exception 異常
     */
    @Test
    public void testBoot() throws Exception {
        final NetworkManager instance = new NetworkManager(this.reportQueue, this.network, timeout, this.peerDriver, this.addressDriver);
        this.executor.submit(instance);
        Thread.sleep(100);
        this.executor.shutdownNow();

        Assert.assertTrue(this.executor.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS));
        Assert.assertNull(this.reportQueue.poll());
    }

}
