/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.CommunicationError;

/**
 * 通信異常への対処。
 * @author chirauraNoSakusha
 */
final class CommunicationErrorDriver {

    private static final Logger LOG = Logger.getLogger(CommunicationErrorDriver.class.getName());

    private final NetworkWrapper network;

    CommunicationErrorDriver(final NetworkWrapper network) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        }

        this.network = network;
    }

    void execute(final CommunicationError error) {
        if (error.getError() instanceof MyRuleException) {
            if (this.network.removeInvalidPeer(error.getDestination())) {
                LOG.log(Level.FINER, "不正な個体 {0} を除外しました。", error.getDestination());
            } else {
                LOG.log(Level.FINEST, "不正な個体 {0} を検知しました。", error.getDestination());
            }
        } else {
            if (this.network.removePeer(error.getDestination())) {
                LOG.log(Level.FINER, "{0} を通信網から外しました。", error.getDestination());
            } else {
                LOG.log(Level.FINEST, "{0} の異常を検知しました。", error.getDestination());
            }
        }
    }

}
