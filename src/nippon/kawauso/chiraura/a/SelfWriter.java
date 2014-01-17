/**
 * 
 */
package nippon.kawauso.chiraura.a;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.p2p.SelfReport;
import nippon.kawauso.chiraura.gui.Gui;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class SelfWriter extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(SelfWriter.class.getName());

    private final Environment environment;
    private final BlockingQueue<SelfReport> selfReportSource;
    private final Gui gui;

    SelfWriter(final BlockingQueue<? super Reporter.Report> reportSink, final Environment environment, final BlockingQueue<SelfReport> selfReportSource,
            final Gui gui) {
        super(reportSink);

        if (environment == null) {
            throw new IllegalArgumentException("Null environment.");
        } else if (selfReportSource == null) {
            throw new IllegalArgumentException("Null self report source.");
        }

        this.environment = environment;
        this.selfReportSource = selfReportSource;
        this.gui = gui;
    }

    @Override
    protected Void subCall() throws InterruptedException {
        Environment.Self cur = this.environment.loadSelf();

        while (!Thread.currentThread().isInterrupted()) {
            final SelfReport report = this.selfReportSource.take();

            if (cur == null || !report.get().equals(cur.get())) {
                if (cur == null) {
                    LOG.log(Level.FINER, "自分の個体情報が {0} に決定しました。", report.get());
                } else {
                    LOG.log(Level.WARNING, "自分の個体情報が {0} から {1} に変わりました。", new Object[] { cur.get(), report.get() });
                }
                try {
                    cur = this.environment.storeSelf(report.get());
                } catch (final IOException e) {
                    LOG.log(Level.WARNING, "公開用個体情報の書き出しに失敗しました", e);
                }
            }
            if (cur != null && this.gui != null) {
                this.gui.setSelf(cur.get(), cur.getPublicForm());
            }
        }

        return null;
    }

}
