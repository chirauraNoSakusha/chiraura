package nippon.kawauso.chiraura.closet.p2p;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;

/**
 * @author chirauraNoSakusha
 */
final class ClosePortWarningDriver {
    private static final Logger LOG = Logger.getLogger(ClosePortWarningDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final BlockingQueue<OutlawReport> outlawReportSink;

    ClosePortWarningDriver(final NetworkWrapper network, final BlockingQueue<OutlawReport> outlawReportSink) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (outlawReportSink == null) {
            throw new IllegalArgumentException("Null outlaw report sink.");
        }
        this.network = network;
        this.outlawReportSink = outlawReportSink;
    }

    void execute(final nippon.kawauso.chiraura.messenger.ClosePortWarning report) {
        LOG.log(Level.FINEST, "{0} から警告されました。", report.getDestination());

        // 逆ギレする。
        this.network.removeLostPeer(report.getDestination());
        ConcurrentFunctions.completePut(new OutlawReport(report.getDestination()), this.outlawReportSink);
    }

}
