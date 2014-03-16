package nippon.kawauso.chiraura.lib.process;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.process.Reporter.Report;
import nippon.kawauso.chiraura.lib.test.TestFunctions;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class ChiefTest {

    final BlockingQueue<Reporter.Report> reportQueue;
    private final ExecutorService executor;

    /**
     * 初期化。
     */
    public ChiefTest() {
        TestFunctions.testLogging(this.getClass().getSimpleName());
        this.reportQueue = new LinkedBlockingQueue<>();
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * 後片付け。
     * @throws Exception 異常
     */
    @After
    public void after() throws Exception {
        this.executor.shutdownNow();
        Assert.assertTrue(this.executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
    }

    /**
     * ちゃんと通知が来るかどうかの検査。
     * @throws Exception 異常
     */
    @Test
    public void testError() throws Exception {
        final boolean[] result = new boolean[] { false };
        this.executor.submit(new Chief(this.reportQueue) {
            @Override
            protected void reaction(final Report report) throws Exception {
                result[0] = (report.getSource() == ChiefTest.this.getClass()) && report.getCause().getClass() == RuntimeException.class
                        && report.getCause().getMessage().equals("Test.");
            }
        });

        final Reporter.Report report = new Reporter.Report(this.getClass(), new RuntimeException("Test."));
        this.reportQueue.put(report);
    }

    /**
     * ちゃんと通知が来るかどうかの検査。
     * @throws Exception 異常
     */
    @Test
    public void testReport() throws Exception {
        final AtomicBoolean flag = new AtomicBoolean(false);
        this.executor.submit(new Chief(this.reportQueue) {
            @Override
            protected void reaction(final Report report) throws Exception {
                flag.set(true);
            }
        });

        final Reporter.Report report = new Reporter.Report(this.getClass(), new RuntimeException("Test."));
        this.reportQueue.put(report);

        Thread.sleep(100);

        Assert.assertTrue(flag.get());
    }

}
