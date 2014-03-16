package nippon.kawauso.chiraura.closet.p2p;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import nippon.kawauso.chiraura.closet.ClosetReport;
import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.storage.Chunk;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class MessengerMonitorTest {

    private static final long versionGapThreshold = 1L;
    private static final boolean portIgnore = true;
    private static final int entryLimit = 10;

    private final Random random;
    private final BlockingQueue<Reporter.Report> reportQueue;
    private final BlockingQueue<ClosetReport> closerReportQueue;
    private final BlockingQueue<Operation> operationQueue;
    private final ExecutorService executor;
    private final MessengerMonitor instance;
    private final long shutdownTimeout;

    /**
     * 初期化。
     */
    public MessengerMonitorTest() {
        this.random = new Random();
        this.reportQueue = new LinkedBlockingQueue<>();
        this.closerReportQueue = new LinkedBlockingQueue<>();
        this.operationQueue = new LinkedBlockingQueue<>();
        this.executor = Executors.newCachedThreadPool();
        final NetworkWrapper network = NetworkWrapperTest.sample(this.random, new HashingCalculator(1_000));
        final StorageWrapper storage = StorageWrapperTest.sample(this.random, this.operationQueue);

        final Set<Class<? extends Chunk>> backupTypes = new HashSet<>();
        final DriverSet drivers = new DriverSet(network, storage, new SessionManager(), this.operationQueue, new LinkedBlockingQueue<OutlawReport>(),
                this.executor, portIgnore, entryLimit, backupTypes);

        this.instance = new MessengerMonitor(this.reportQueue, network, this.closerReportQueue, versionGapThreshold, drivers);
        this.shutdownTimeout = Duration.SECOND;
    }

    /**
     * 起動試験。
     * @throws Exception 異常
     */
    @Test
    public void testBoot() throws Exception {
        this.executor.submit(this.instance);
        Thread.sleep(100);
        this.executor.shutdownNow();

        Assert.assertTrue(this.executor.awaitTermination(this.shutdownTimeout, TimeUnit.MILLISECONDS));
        Assert.assertNull(this.reportQueue.poll());
    }

}
