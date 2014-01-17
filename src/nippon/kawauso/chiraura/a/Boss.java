/**
 * 
 */
package nippon.kawauso.chiraura.a;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Closet;
import nippon.kawauso.chiraura.closet.p2p.SelfReport;
import nippon.kawauso.chiraura.gui.Gui;
import nippon.kawauso.chiraura.lib.process.Chief;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class Boss extends Chief {

    private static final Logger LOG = Logger.getLogger(Boss.class.getName());

    private final Closet closet;

    private final Gui gui;
    private final CountDownLatch shutdownStopper;

    private final Environment environment;

    private final ExecutorService executor;

    private final BlockingQueue<SelfReport> selfReportQueue;

    Boss(final Closet closet, final Gui gui, final CountDownLatch shutdownStopper, final Environment environment, final ExecutorService executor) {
        super(new LinkedBlockingQueue<Reporter.Report>());

        if (closet == null) {
            throw new IllegalArgumentException("Null closet.");
        } else if (shutdownStopper == null) {
            throw new IllegalArgumentException("Null shutdown stopper.");
        } else if (environment == null) {
            throw new IllegalArgumentException("Null environment.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        }

        this.closet = closet;
        this.gui = gui;
        this.shutdownStopper = shutdownStopper;
        this.environment = environment;
        this.executor = executor;

        this.selfReportQueue = new LinkedBlockingQueue<>();
    }

    private ClosetMonitor newClosetMonitor() {
        return new ClosetMonitor(getReportQueue(), this.closet, this.gui, this.selfReportQueue);
    }

    private GuiMonitor newGuiMonitor() {
        return new GuiMonitor(getReportQueue(), this.gui, this.shutdownStopper);
    }

    private SelfWriter newSelfWriter() {
        return new SelfWriter(getReportQueue(), this.environment, this.selfReportQueue, this.gui);
    }

    @Override
    protected void before() {
        this.executor.submit(newClosetMonitor());
        if (this.gui != null) {
            this.executor.submit(newGuiMonitor());
        }
        this.executor.submit(newSelfWriter());
    }

    @Override
    protected void reaction(final Reporter.Report report) throws Exception {
        boolean done = true;
        if (report.getSource() == ClosetMonitor.class) {
            this.executor.submit(newClosetMonitor());
        } else if (report.getSource() == GuiMonitor.class) {
            if (this.gui != null) {
                this.executor.submit(newGuiMonitor());
            } else {
                LOG.log(Level.WARNING, "おかしな奴 {0} から報告が来ました。", report.getSource().getName());
            }
        } else if (report.getSource() == SelfWriter.class) {
            this.executor.submit(newSelfWriter());
        } else {
            done = false;
        }

        if (done) {
            LOG.log(Level.WARNING, report.getSource().getName() + " を再起動しました", report.getCause());
        } else {
            LOG.log(Level.WARNING, "知らない奴から報告 {0} が来ました。", report);
        }
    }

}
