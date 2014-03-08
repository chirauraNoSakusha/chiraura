/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class SimpleRecoveryMessageDriver {

    private static final Logger LOG = Logger.getLogger(SimpleRecoveryMessageDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final TypeRegistry<Chunk> chunkRegistry;

    SimpleRecoveryMessageDriver(final NetworkWrapper network, final StorageWrapper storage, final TypeRegistry<Chunk> chunkRegistry) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        } else if (chunkRegistry == null) {
            throw new IllegalArgumentException("Null chunk registry.");
        }
        this.network = network;
        this.storage = storage;
        this.chunkRegistry = chunkRegistry;
    }

    void execute(final SimpleRecoveryMessage message, final Session session, final PublicKey sourceId, final InetSocketAddress source)
            throws InterruptedException {
        final Address address = message.getId().getAddress();

        final List<Message> reply = new ArrayList<>(2);
        final int connectionType;
        if (this.network.moreAppropriate(address, sourceId)) {
            LOG.log(Level.FINEST, "{0} に依頼された {1} を拒否します。", new Object[] { source, message });
            reply.add(SimpleRecoveryReply.newRejected());
            connectionType = ConnectionTypes.CONTROL;
        } else {
            // データ片の準備。
            Chunk chunk = null;
            try {
                chunk = this.storage.read(message.getId());
            } catch (final IOException e) {
                LOG.log(Level.WARNING, "異常が発生しました", e);
                LOG.log(Level.INFO, "{0} を読み込めませんでした。", message.getId());
            }

            if (chunk != null) {
                LOG.log(Level.FINEST, "{0} に依頼された {1} が成功しました。", new Object[] { source, message });
                reply.add(new SimpleRecoveryReply(this.chunkRegistry, chunk));
                connectionType = ConnectionTypes.DATA;
            } else {
                LOG.log(Level.FINEST, "{0} に依頼された {1} は対象無しでした。", new Object[] { source, message });
                reply.add(SimpleRecoveryReply.newNotFound());
                connectionType = ConnectionTypes.CONTROL;
            }
        }
        reply.add(new SessionReply(session));

        // 返信する。
        this.network.sendMail(source, connectionType, reply);
    }
}
