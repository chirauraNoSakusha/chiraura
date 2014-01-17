package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.AcceptanceError;

/**
 * 接続先特定中の異常を捌く。
 * @author chirauraNoSakusha
 */
final class AcceptanceErrorDriver {

    private static final Logger LOG = Logger.getLogger(AcceptanceErrorDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;

    AcceptanceErrorDriver(final NetworkWrapper network) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        }

        this.network = network;
    }

    void execute(final AcceptanceError error) {
        if (error.getError() instanceof MyRuleException) {
            if (this.network.removeInvalidPeer(error.getDestination())) {
                LOG.log(Level.FINER, "不正な個体 " + error.getDestination() + " を除外しました", error.getError());
            } else {
                LOG.log(Level.FINEST, "不正な個体 " + error.getDestination() + " を検知しました", error.getError());
            }
        } else {
            if (this.network.removePeer(error.getDestination())) {
                LOG.log(Level.FINER, error.getDestination() + " を通信網から外しました", error.getError());
            } else {
                LOG.log(Level.FINEST, error.getDestination() + " の異常を検知しました", error.getError());
            }
        }
    }

}
