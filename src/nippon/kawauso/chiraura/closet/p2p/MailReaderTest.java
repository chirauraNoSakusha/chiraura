package nippon.kawauso.chiraura.closet.p2p;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.storage.Chunk;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class MailReaderTest {

    private static final boolean portIgnore = true;
    private static final int entryLimit = 10;

    private static final long timeout = Duration.SECOND;
    private static final long shutdownTimeout = Duration.SECOND;

    private final Random random;
    private final BlockingQueue<Reporter.Report> reportQueue;
    private final BlockingQueue<Operation> operationQueue;
    private final NetworkWrapper network;
    private final SessionManager sessionManager;
    private final ExecutorService executor;
    private final DriverSet drivers;

    /**
     * 初期化。
     */
    public MailReaderTest() {
        this.random = new Random();
        this.reportQueue = new LinkedBlockingQueue<>();
        this.operationQueue = new LinkedBlockingQueue<>();
        this.network = NetworkWrapperTest.sample(this.random, new HashingCalculator(1_000));
        this.sessionManager = new SessionManager();
        this.executor = Executors.newCachedThreadPool();

        final StorageWrapper storage = StorageWrapperTest.sample(this.random, this.operationQueue);
        final Set<Class<? extends Chunk>> backupTypes = new HashSet<>();
        this.drivers = new DriverSet(this.network, storage, this.sessionManager, new LinkedBlockingQueue<Operation>(), new LinkedBlockingQueue<OutlawReport>(),
                this.executor, portIgnore, entryLimit, backupTypes);
    }

    /**
     * 起動試験。
     * @throws Exception 異常
     */
    @Test
    public void testBoot() throws Exception {
        final MailReader instance = new MailReader(this.reportQueue, this.network, this.sessionManager, timeout, this.drivers, this.drivers);
        this.executor.submit(instance);
        Thread.sleep(100);
        this.executor.shutdownNow();

        Assert.assertTrue(this.executor.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS));
        Assert.assertNull(this.reportQueue.poll());
    }
}
