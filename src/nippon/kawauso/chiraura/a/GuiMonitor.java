/**
 * 
 */
package nippon.kawauso.chiraura.a;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.gui.Gui;
import nippon.kawauso.chiraura.gui.GuiCommand;
import nippon.kawauso.chiraura.gui.ShutdownCommand;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class GuiMonitor extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(GuiMonitor.class.getName());

    private final Gui gui;

    private final CountDownLatch shutdownStopper;

    protected GuiMonitor(final BlockingQueue<? super Reporter.Report> reportSink, final Gui gui, final CountDownLatch shutdownStopper) {
        super(reportSink);

        if (gui == null) {
            throw new IllegalArgumentException("Null gui.");
        } else if (shutdownStopper == null) {
            throw new IllegalArgumentException("Null shutdown stopper.");
        }

        this.gui = gui;
        this.shutdownStopper = shutdownStopper;
    }

    @Override
    protected Void subCall() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            final GuiCommand task = this.gui.take();
            boolean done = true;
            if (task instanceof ShutdownCommand) {
                this.shutdownStopper.countDown();
            } else {
                done = false;
            }

            if (done) {
                LOG.log(Level.FINER, "{0} を捌きました。", task);
            } else {
                LOG.log(Level.WARNING, "{0} の処理は実装されていません。", task);
            }
        }

        return null;
    }
}
