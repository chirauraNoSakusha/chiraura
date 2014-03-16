package nippon.kawauso.chiraura.closet.p2p;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.ClosetReport;
import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.messenger.AcceptanceError;
import nippon.kawauso.chiraura.messenger.CommunicationError;
import nippon.kawauso.chiraura.messenger.ConnectReport;
import nippon.kawauso.chiraura.messenger.ContactError;
import nippon.kawauso.chiraura.messenger.MessengerReport;
import nippon.kawauso.chiraura.messenger.UnsentMail;

/**
 * 配達係が起こした問題に対処する人。
 * @author chirauraNoSakusha
 */
final class MessengerMonitor extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(MessengerMonitor.class.getName());

    // 参照。
    private final NetworkWrapper errorSource;
    private final BlockingQueue<ClosetReport> closetReportSink;

    private final long versionGapThreshold;

    // 保持。
    private final MessengerReportDriverSet drivers;

    MessengerMonitor(final BlockingQueue<Reporter.Report> reportSink, final NetworkWrapper errorSource, final BlockingQueue<ClosetReport> closetReportSink,
            final long versionGapThreshold, final MessengerReportDriverSet drivers) {
        super(reportSink);

        if (errorSource == null) {
            throw new IllegalArgumentException("Null error source.");
        } else if (closetReportSink == null) {
            throw new IllegalArgumentException("Null closet report sink.");
        } else if (versionGapThreshold < 1) {
            throw new IllegalArgumentException("Too small version gap threshold ( " + versionGapThreshold + " ).");
        } else if (drivers == null) {
            throw new IllegalArgumentException("Null drivers.");
        }
        this.errorSource = errorSource;
        this.closetReportSink = closetReportSink;
        this.versionGapThreshold = versionGapThreshold;
        this.drivers = drivers;
    }

    @Override
    protected Void subCall() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            final MessengerReport report = this.errorSource.takeMessengerReport();
            boolean done = true;
            if (report instanceof ConnectReport) {
                this.drivers.getConnectReport().execute((ConnectReport) report);
            } else if (report instanceof CommunicationError) {
                this.drivers.getCommunicationError().execute((CommunicationError) report);
            } else if (report instanceof ContactError) {
                this.drivers.getContactError().execute((ContactError) report);
            } else if (report instanceof AcceptanceError) {
                this.drivers.getAcceptanceError().execute((AcceptanceError) report);
            } else if (report instanceof UnsentMail) {
                this.drivers.getUnsentMail().execute((UnsentMail) report);
            } else if (report instanceof nippon.kawauso.chiraura.messenger.ServerError) {
                ConcurrentFunctions.completePut(new ServerError((nippon.kawauso.chiraura.messenger.ServerError) report), this.closetReportSink);
            } else if (report instanceof nippon.kawauso.chiraura.messenger.ClosePortWarning) {
                this.drivers.getClosePortWarning().execute((nippon.kawauso.chiraura.messenger.ClosePortWarning) report);
                ConcurrentFunctions.completePut(new ClosePortWarning((nippon.kawauso.chiraura.messenger.ClosePortWarning) report), this.closetReportSink);
            } else if (report instanceof nippon.kawauso.chiraura.messenger.NewProtocolWarning) {
                final nippon.kawauso.chiraura.messenger.NewProtocolWarning lowWarning = (nippon.kawauso.chiraura.messenger.NewProtocolWarning) report;
                final long diff = lowWarning.getNewVersion() - lowWarning.getVersion();
                final long majorDiff = diff / this.versionGapThreshold;
                final long minorDiff = diff % this.versionGapThreshold;
                ConcurrentFunctions.completePut(new NewProtocolWarning(majorDiff, minorDiff), this.closetReportSink);
            } else if (report instanceof nippon.kawauso.chiraura.messenger.SelfReport) {
                ConcurrentFunctions.completePut(new SelfReport((nippon.kawauso.chiraura.messenger.SelfReport) report), this.closetReportSink);
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
