package nippon.kawauso.chiraura.lib.process;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;

/**
 * 異常を報告する作業員。
 * @author chirauraNoSakusha
 * @param <T> 返り値のクラス
 */
public abstract class Reporter<T> implements Callable<T> {

    private static final Logger LOG = Logger.getLogger(Reporter.class.getName());

    /**
     * 報告書。
     * @author chirauraNoSakusha
     */
    public static final class Report {
        private final Class<?> source;
        private final Exception cause;

        protected Report(final Class<?> source, final Exception cause) {
            this.source = source;
            this.cause = cause;
        }

        /**
         * 報告者のクラスを返す。
         * @return 報告者のクラス
         */
        public Class<?> getSource() {
            return this.source;
        }

        /**
         * 異常を返す。
         * @return 異常
         */
        public Exception getCause() {
            return this.cause;
        }

        @Override
        public String toString() {
            return new StringBuilder(this.getClass().getSimpleName())
                    .append('[').append(this.source.getName())
                    .append(':').append(System.lineSeparator()).append(LoggingFunctions.getStackTraceString(this.cause))
                    .append(']').toString();
        }
    }

    private final BlockingQueue<? super Report> reportSink;
    private final Level reportLevel;

    private static final Level DEFAULT_LEVEL = Level.SEVERE;

    private Reporter(final BlockingQueue<? super Report> reportSink, final Level reportLevel) {
        this.reportSink = reportSink;
        this.reportLevel = reportLevel;
    }

    /**
     * キューに報告書を提出するように作成する。
     * @param reportSink 報告書を提出するキュー
     */
    protected Reporter(final BlockingQueue<? super Report> reportSink) {
        this(reportSink, DEFAULT_LEVEL);
    }

    /**
     * 報告書をログとして表示するように作成する。
     * @param reportLevel 報告書のログレベル
     */
    protected Reporter(final Level reportLevel) {
        this(null, reportLevel);
    }

    /**
     * 作業内容。
     * @return 任意の結果
     * @throws Exception 異常
     */
    protected abstract T subCall() throws Exception;

    /**
     * 報告書を作る。
     * @param e 原因となった異常
     * @return 報告書
     */
    protected Report newReport(final Exception e) {
        return new Report(this.getClass(), e);
    }

    @Override
    public final T call() {
        LOG.logp(Level.FINE, this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(), "こんにちは。");

        try {
            return subCall();
        } catch (final InterruptedException ignored) {
            // 終了。
        } catch (final Exception e) {
            // 報告する。
            if (this.reportSink != null) {
                ConcurrentFunctions.completePut(newReport(e), this.reportSink);
            } else {
                LOG.log(this.reportLevel, newReport(e).toString());
            }
        } finally {
            LOG.logp(Level.FINE, this.getClass().getName(), Thread.currentThread().getStackTrace()[1].getMethodName(), "さようなら。");
        }
        return null;
    }

}
