/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * 論理位置の支配者確認への応答を捌く。
 * @author chirauraNoSakusha
 */
final class AddressAccessReplyDriver {

    private static final Logger LOG = Logger.getLogger(AddressAccessReplyDriver.class.getName());

    private final NetworkWrapper network;

    AddressAccessReplyDriver(final NetworkWrapper network) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        }
        this.network = network;
    }

    void execute(final AddressAccessReply reply) {
        if (reply.isRejected() || reply.isGivenUp()) {
            LOG.log(Level.FINEST, "{0} に対してすることはありません。", reply);
        } else {
            boolean success = false;
            if (reply.getManager() != null) {
                if (addPeer(reply.getManager())) {
                    success = true;
                }
            }
            for (final AddressedPeer peer : reply.getPeers()) {
                if (addPeer(peer)) {
                    success = true;
                }
            }
            if (success) {
                LOG.log(Level.FINEST, "{0} による更新がありました。", reply);
            } else {
                LOG.log(Level.FINEST, "{0} による更新がありませんでした。", reply);
            }
        }
    }

    private boolean addPeer(final AddressedPeer peer) {
        if (this.network.addPeer(peer)) {
            LOG.log(Level.FINER, "{0} を通信網に加えました。", peer);
            return true;
        } else {
            return false;
        }
    }

}
