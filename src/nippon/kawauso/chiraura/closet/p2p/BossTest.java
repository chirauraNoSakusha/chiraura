/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import nippon.kawauso.chiraura.closet.ClosetReport;
import nippon.kawauso.chiraura.lib.Duration;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class BossTest {

    private static final long interval = 500L;
    private static final long sleepTime = 10 * Duration.SECOND;
    private static final long backupInterval = Duration.SECOND;
    private static final long timeout = 10 * Duration.SECOND;
    private static final long versionGapThreshold = 1L;
    private final Random random;
    private final BlockingQueue<Operation> operationQueue;
    private final BlockingQueue<ClosetReport> closetReportQueue;
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final SessionManager sessionManager;
    private final ExecutorService executor;

    private final DriverSet drivers;

    /**
     * 初期化。
     */
    public BossTest() {
        this.random = new Random();
        this.operationQueue = new LinkedBlockingQueue<>();
        this.closetReportQueue = new LinkedBlockingQueue<>();
        this.network = NetworkWrapperTest.sample(this.random, new HashingCalculator(1_000));
        this.storage = StorageWrapperTest.sample(this.random, this.operationQueue);
        this.sessionManager = new SessionManager();
        this.executor = Executors.newCachedThreadPool();

        this.drivers = new DriverSet(this.network, this.storage, this.sessionManager, new LinkedBlockingQueue<Operation>(), this.executor);
    }

    /**
     * 起動試験。
     * @throws Exception 異常
     */
    @Test
    public void testBoot() throws Exception {
        final Boss instance = new Boss(this.network, this.sessionManager, interval, sleepTime, backupInterval, timeout, versionGapThreshold, this.executor,
                this.operationQueue, this.closetReportQueue, this.drivers);
        final Future<Void> future = this.executor.submit(instance);
        Thread.sleep(100);
        this.executor.shutdownNow();

        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        future.get(Duration.SECOND, TimeUnit.MILLISECONDS);
    }

}
