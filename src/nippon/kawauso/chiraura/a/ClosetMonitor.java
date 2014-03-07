/**
 * 
 */
package nippon.kawauso.chiraura.a;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Closet;
import nippon.kawauso.chiraura.closet.ClosetReport;
import nippon.kawauso.chiraura.closet.p2p.ClosePortWarning;
import nippon.kawauso.chiraura.closet.p2p.NewProtocolWarning;
import nippon.kawauso.chiraura.closet.p2p.SelfReport;
import nippon.kawauso.chiraura.closet.p2p.ServerError;
import nippon.kawauso.chiraura.gui.Gui;
import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class ClosetMonitor extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(ClosetMonitor.class.getName());

    private final Closet closet;
    private final Gui gui;
    private final BlockingQueue<SelfReport> selfReportSink;

    protected ClosetMonitor(final BlockingQueue<? super Reporter.Report> reportSink, final Closet closet, final Gui gui,
            final BlockingQueue<SelfReport> selfReportSink) {
        super(reportSink);

        if (closet == null) {
            throw new IllegalArgumentException("Null closet.");
        } else if (selfReportSink == null) {
            throw new IllegalArgumentException("Null self report sink.");
        }

        this.closet = closet;
        this.gui = gui;
        this.selfReportSink = selfReportSink;
    }

    @Override
    protected Void subCall() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            final ClosetReport report = this.closet.takeError();
            boolean done = true;
            if (report instanceof ServerError) {
                LOG.log(Level.SEVERE, "サーバが起動できません", ((ServerError) report).getError());
                if (this.gui != null) {
                    this.gui.displayServerError();
                }
            } else if (report instanceof ClosePortWarning) {
                final ClosePortWarning report0 = (ClosePortWarning) report;
                LOG.log(Level.WARNING, "ポート {0} が開いていないかもしれません。", Integer.toString(report0.getPort()));
                if (this.gui != null) {
                    this.gui.displayClosePortWarning(report0.getPort(), report0.getDestination());
                }
            } else if (report instanceof NewProtocolWarning) {
                final NewProtocolWarning report0 = (NewProtocolWarning) report;
                LOG.log(Level.WARNING, "この個体より {0} 段階と {1} 歩だけ新しい個体がいるようです。",
                        new Object[] { Long.toString(report0.getMajorGap()), Long.toString(report0.getMinorGap()) });
                if (this.gui != null) {
                    this.gui.displayVersionGapWarning(report0.getMajorGap(), report0.getMinorGap());
                }
            } else if (report instanceof SelfReport) {
                ConcurrentFunctions.completePut((SelfReport) report, this.selfReportSink);
            } else {
                done = false;
            }

            if (done) {
                LOG.log(Level.FINER, "{0} を捌きました。", report);
            } else {
                LOG.log(Level.WARNING, "{0} の処理は実装されていません。", report);
            }
        }

        return null;
    }

}
