/**
 * 
 */
package nippon.kawauso.chiraura.lib.process;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter.Report;

/**
 * 異常を報告するプロセスを統括するプロセス。
 * @author chirauraNoSakusha
 */
public abstract class Chief implements Callable<Void> {

    private static final Logger LOG = Logger.getLogger(Chief.class.getName());

    private final BlockingQueue<Reporter.Report> reportSource;

    protected Chief(final BlockingQueue<Reporter.Report> reportSource) {
        if (reportSource == null) {
            throw new IllegalArgumentException("Null report source.");
        }
        this.reportSource = reportSource;
    }

    /**
     * 報告書のキューを返す。
     * @return 報告書のキュー
     */
    protected BlockingQueue<Reporter.Report> getReportQueue() {
        return this.reportSource;
    }

    /**
     * 起動直後の処理。
     * @throws Exception 異常
     */
    protected void before() throws Exception {}

    /**
     * 終了間際の処理。
     * @throws Exception 異常
     */
    protected void after() throws Exception {}

    /**
     * 報告書に対する処理。
     * @param report 報告書
     * @throws Exception 異常
     */
    protected abstract void reaction(Report report) throws Exception;

    @Override
    public final Void call() throws Exception {
        LOG.logp(Level.FINE, this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(), "こんにちは。");

        try {
            before();
        } catch (final Exception e) {
            LOG.logrb(Level.WARNING, this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(), null,
                    "開始処理中に異常が発生しました。", e);
            // e.printStackTrace();
        }

        try {
            while (!Thread.currentThread().isInterrupted()) {
                Reporter.Report report;
                try {
                    report = this.reportSource.take();
                } catch (final InterruptedException e) {
                    break;
                }
                try {
                    reaction(report);
                } catch (final InterruptedException e) {
                    break;
                } catch (final Exception e) {
                    LOG.logrb(Level.FINE, this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(), null,
                            "報告 " + report + " の処理中に異常が発生しました。", e);
                }
            }
        } finally {
            after();
            LOG.logp(Level.FINE, this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(), "さようなら。");
        }

        return null;
    }
}
