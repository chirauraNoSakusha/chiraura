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
public final class LonelyTest {

    private static final long minInterval = Duration.SECOND;
    private static final long maxInterval = 2 * Duration.SECOND;
    private static final long timeout = Duration.SECOND;
    private static final long shutdownTimeout = Duration.SECOND;

    private final Random random;
    private final BlockingQueue<Reporter.Report> reportQueue;
    private final NetworkWrapper network;
    private final FirstAccessSelectDriver driver;
    private final ExecutorService executor;

    /**
     * 初期化。
     */
    public LonelyTest() {
        this.random = new Random();
        this.reportQueue = new LinkedBlockingQueue<>();
        this.network = NetworkWrapperTest.sample(this.random, new HashingCalculator(1_000));
        this.executor = Executors.newCachedThreadPool();
        final SessionManager sessionManager = new SessionManager();
        final FirstAccessDriver coreDriver = new FirstAccessDriver(sessionManager, this.network, new LinkedBlockingQueue<OutlawReport>());
        this.driver = new FirstAccessSelectDriver(new OperationAggregator<FirstAccessOperation, FirstAccessResult>(), coreDriver);
    }

    /**
     * 起動試験。
     * @throws Exception 異常
     */
    @Test
    public void testBoot() throws Exception {
        final Lonely instance = new Lonely(this.reportQueue, this.network, this.executor, minInterval, maxInterval, timeout, this.driver);
        this.executor.submit(instance);
        Thread.sleep(100);
        this.executor.shutdownNow();

        Assert.assertTrue(this.executor.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS));
        Assert.assertNull(this.reportQueue.poll());
    }

}
