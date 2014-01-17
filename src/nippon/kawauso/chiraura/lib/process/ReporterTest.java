/**
 * 
 */
package nippon.kawauso.chiraura.lib.process;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class ReporterTest {

    final BlockingQueue<Reporter.Report> reportQueue;
    private final ExecutorService executor;

    /**
     * 初期化。
     */
    public ReporterTest() {
        this.reportQueue = new LinkedBlockingQueue<>();
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * ちゃんと通知が来るかどうかの検査。
     * @throws Exception 異常
     */
    @Test
    public void testReport() throws Exception {
        this.executor.submit(new Reporter<Void>(this.reportQueue) {
            @Override
            protected Void subCall() throws Exception {
                throw new RuntimeException("Test.");
            }
        });

        final Reporter.Report report = this.reportQueue.poll(1, TimeUnit.SECONDS);
        Assert.assertNotNull(report);
        Assert.assertEquals(RuntimeException.class, report.getCause().getClass());
        // System.out.println(report);
    }

}
