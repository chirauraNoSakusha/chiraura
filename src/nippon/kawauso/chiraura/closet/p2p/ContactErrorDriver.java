package nippon.kawauso.chiraura.closet.p2p;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.ContactError;

/**
 * 接続開始中の異常への対処。
 * @author chirauraNoSakusha
 */
final class ContactErrorDriver {

    private static final Logger LOG = Logger.getLogger(ContactErrorDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final BlockingQueue<OutlawReport> outlawReportSink;

    ContactErrorDriver(final NetworkWrapper network, final BlockingQueue<OutlawReport> outlawReportSink) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (outlawReportSink == null) {
            throw new IllegalArgumentException("Null outlaw report sink.");
        }

        this.network = network;
        this.outlawReportSink = outlawReportSink;
    }

    void execute(final ContactError error) {
        ConcurrentFunctions.completePut(new OutlawReport(error.getDestination()), this.outlawReportSink);
        if (error.getError() instanceof MyRuleException) {
            LOG.log(Level.FINEST, "不正な個体 {0} を検知しました。", error.getDestination());
        } else if (error.getError() instanceof ConnectException || error.getError() instanceof NoRouteToHostException) {
            if (this.network.removeLostPeer(error.getDestination())) {
                LOG.log(Level.FINER, "接続不能個体 {0} を除外しました。", error.getDestination());
            } else {
                LOG.log(Level.FINEST, "接続不能個体 {0} を検知しました。", error.getDestination());
            }
        } else {
            LOG.log(Level.FINEST, "{0} の異常を検知しました。", error.getDestination());
        }
    }

}
