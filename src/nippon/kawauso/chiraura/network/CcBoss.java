/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Chief;
import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.lib.process.Reporter.Report;

/**
 * CustomChord を維持する。
 * @author chirauraNoSakusha
 */
final class CcBoss extends Chief {

    private static final Logger LOG = Logger.getLogger(CcBoss.class.getName());

    // 参照。
    private final ExecutorService executor;
    private final CcView view;
    private final long maintenanceInterval;
    private final BlockingQueue<NetworkTask> taskSink;

    CcBoss(final ExecutorService executor, final CcView view, final long maintenanceInterval, final BlockingQueue<NetworkTask> taskSink) {
        super(new LinkedBlockingQueue<Reporter.Report>());

        if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        } else if (view == null) {
            throw new IllegalArgumentException("Null view.");
        } else if (maintenanceInterval < 0) {
            throw new IllegalArgumentException("Negative maintenance interval ( " + maintenanceInterval + " ).");
        } else if (taskSink == null) {
            throw new IllegalArgumentException("Null task sink.");
        }

        this.executor = executor;
        this.view = view;
        this.maintenanceInterval = maintenanceInterval;
        this.taskSink = taskSink;
    }

    private CcSuccessorStabilizer newCcSuccessorStabilizer() {
        return new CcSuccessorStabilizer(getReportQueue(), this.maintenanceInterval, this.view, this.taskSink);
    }

    private CcFingerStabilizer newCcFingerStabilizer() {
        return new CcFingerStabilizer(getReportQueue(), this.maintenanceInterval, this.view, this.taskSink);
    }

    private CcFingerDigger newCcFingerDigger() {
        return new CcFingerDigger(getReportQueue(), this.maintenanceInterval, this.view, this.taskSink);
    }

    @Override
    protected void before() {
        this.executor.submit(newCcSuccessorStabilizer());
        this.executor.submit(newCcFingerStabilizer());
        this.executor.submit(newCcFingerDigger());
    }

    @Override
    protected void reaction(final Report report) {
        boolean done = true;
        if (report.getSource() == CcSuccessorStabilizer.class) {
            this.executor.submit(newCcSuccessorStabilizer());
        } else if (report.getSource() == CcFingerStabilizer.class) {
            this.executor.submit(newCcFingerStabilizer());
        } else if (report.getSource() == CcFingerDigger.class) {
            this.executor.submit(newCcFingerDigger());
        } else {
            done = false;
        }

        if (done) {
            LOG.log(Level.WARNING, report.getSource().getName() + " を再起動しました。", report.getCause());
        } else {
            LOG.log(Level.WARNING, "知らない報告 {0} が来ました。", report);
        }
    }

}
