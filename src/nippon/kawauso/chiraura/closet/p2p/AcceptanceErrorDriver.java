package nippon.kawauso.chiraura.closet.p2p;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.AcceptanceError;

/**
 * 接続先特定中の異常を捌く。
 * @author chirauraNoSakusha
 */
final class AcceptanceErrorDriver {

    private static final Logger LOG = Logger.getLogger(AcceptanceErrorDriver.class.getName());

    // 参照。
    private final boolean portIgnore;
    private final BlockingQueue<OutlawReport> outlawReportSink;

    AcceptanceErrorDriver(final boolean portIgnore, final BlockingQueue<OutlawReport> outlawReportSink) {
        if (outlawReportSink == null) {
            throw new IllegalArgumentException("Null outlaw report sink.");
        }

        this.portIgnore = portIgnore;
        this.outlawReportSink = outlawReportSink;
    }

    void execute(final AcceptanceError error) {
        if (this.portIgnore) {
            ConcurrentFunctions.completePut(new OutlawReport(error.getDestination()), this.outlawReportSink);
        }
        if (error.getError() instanceof MyRuleException) {
            LOG.log(Level.FINEST, "不正な個体 {0} を検知しました。", error.getDestination());
        } else {
            LOG.log(Level.FINEST, "{0} の異常を検知しました。", error.getDestination());
        }
    }

}
