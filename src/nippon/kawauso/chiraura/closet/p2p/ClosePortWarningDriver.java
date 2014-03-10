/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class ClosePortWarningDriver {
    private static final Logger LOG = Logger.getLogger(ClosePortWarningDriver.class.getName());

    private final NetworkWrapper network;

    ClosePortWarningDriver(final NetworkWrapper network) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        }

        this.network = network;
    }

    void execute(final nippon.kawauso.chiraura.messenger.ClosePortWarning report) {
        if (this.network.removePeer(report.getDestination())) {
            LOG.log(Level.FINER, "警告者 {0} を通信網から外しました。", report.getDestination());
        } else {
            LOG.log(Level.FINEST, "警告者 {0} がいました。", report.getDestination());
        }
    }

}
