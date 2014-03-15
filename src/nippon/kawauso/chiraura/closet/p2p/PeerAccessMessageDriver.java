package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * 個体確認のための言付けを捌く。
 * @author chirauraNoSakusha
 */
final class PeerAccessMessageDriver {

    private static final Logger LOG = Logger.getLogger(PeerAccessMessageDriver.class.getName());

    private final NetworkWrapper network;

    PeerAccessMessageDriver(final NetworkWrapper network) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        }
        this.network = network;
    }

    void execute(final PeerAccessMessage message, final Session session, final InetSocketAddress source) {
        // 返信内容の用意。
        final List<AddressedPeer> peers = this.network.getImportantPeers();

        // 返信の用意。
        final List<Message> reply = new ArrayList<>(2);
        reply.add(new PeerAccessReply(peers));
        reply.add(new SessionReply(session));

        // 返信する。
        this.network.sendMail(source, ConnectionTypes.DATA, reply);
        LOG.log(Level.FINER, "{0} からの {1} に応えました。", new Object[] { source, message });
    }

}
