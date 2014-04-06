package nippon.kawauso.chiraura.a;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
    protected Void subCall() throws InterruptedException, UnknownHostException {
        Environment.Self cur = this.environment.loadSelf();
        InetAddress source = null;
        if (cur != null) {
            source = InetAddress.getLocalHost();
        }

        while (!Thread.currentThread().isInterrupted()) {
            final SelfReport report = this.selfReportSource.take();

            if (cur == null || !report.getSelf().equals(cur.get())) {
                if (cur == null) {
                    LOG.log(Level.FINER, "自分の個体情報が {0} に決定しました。", report.getSelf());
                } else {
                    LOG.log(Level.WARNING, "自分の個体情報が {0} から {1} に変わりました。", new Object[] { cur.get(), report.getSelf() });
                }
                try {
                    cur = this.environment.storeSelf(report.getSelf());
                    source = report.getDestination().getAddress();
                } catch (final IOException e) {
                    LOG.log(Level.WARNING, "異常が発生しました", e);
                    LOG.log(Level.INFO, "公開用個体情報の書き出しに失敗しました。");
                }
            }
            if (cur != null && this.gui != null) {
                this.gui.setSelf(cur.get(), cur.getPublicForm(), source);
            }
        }

        return null;
    }

}
