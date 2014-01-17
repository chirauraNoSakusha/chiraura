/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.ContactError;

/**
 * 接続開始中の異常への対処。
 * @author chirauraNoSakusha
 */
final class ContactErrorDriver {

    private static final Logger LOG = Logger.getLogger(ContactErrorDriver.class.getName());

    private final NetworkWrapper network;

    ContactErrorDriver(final NetworkWrapper network) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        }

        this.network = network;
    }

    void execute(final ContactError error) {
        if (error.getError() instanceof MyRuleException) {
            if (this.network.removeInvalidPeer(error.getDestination())) {
                LOG.log(Level.FINER, "不正な個体 " + error.getDestination() + " を除外しました", error.getError());
            } else {
                LOG.log(Level.FINEST, "不正な個体 " + error.getDestination() + " を検知しました", error.getError());
            }
        } else if (error.getError() instanceof ConnectException || error.getError() instanceof NoRouteToHostException) {
            if (this.network.removeLostPeer(error.getDestination())) {
                LOG.log(Level.FINER, "接続不能個体 " + error.getDestination() + " を除外しました", error.getError());
            } else {
                LOG.log(Level.FINEST, "接続不能個体 " + error.getDestination() + " を検知しました", error.getError());
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
