/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * 個体確認への応答を捌く。
 * @author chirauraNoSakusha
 */
final class PeerAccessReplyDriver {

    private static final Logger LOG = Logger.getLogger(PeerAccessReplyDriver.class.getName());

    private final NetworkWrapper network;

    PeerAccessReplyDriver(final NetworkWrapper network) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        }
        this.network = network;
    }

    void execute(final PeerAccessReply reply) {
        boolean success = false;
        for (final AddressedPeer peer : reply.getPeers()) {
            if (this.network.addPeer(peer)) {
                success = true;
                LOG.log(Level.FINER, "{0} を通信網に加えました。", peer);
            }
        }
        if (success) {
            LOG.log(Level.FINEST, "{0} による更新がありました。", reply);
        } else {
            LOG.log(Level.FINEST, "{0} による更新がありませんでした。", reply);
        }
    }

}
