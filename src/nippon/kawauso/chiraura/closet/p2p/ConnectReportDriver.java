package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.messenger.ConnectReport;

/**
 * 通信開始報告の処理。
 * @author chirauraNoSakusha
 */
final class ConnectReportDriver {

    private static final Logger LOG = Logger.getLogger(ConnectReportDriver.class.getName());

    private final NetworkWrapper network;

    ConnectReportDriver(final NetworkWrapper network) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        }

        this.network = network;
    }

    void execute(final ConnectReport report) {
        if (this.network.addActivePeer(report.getDestinationId(), report.getDestination())) {
            LOG.log(Level.FINER, "{0} との接続 {1} を確立し、深い仲になりました。", new Object[] { report.getDestination(), Integer.toString(report.getConnectionType()) });
        } else {
            LOG.log(Level.FINER, "{0} との接続 {1} を確立しました。", new Object[] { report.getDestination(), Integer.toString(report.getConnectionType()) });
        }
    }

}
