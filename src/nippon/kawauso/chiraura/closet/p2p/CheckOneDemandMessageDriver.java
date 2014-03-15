package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class CheckOneDemandMessageDriver {

    private static final Logger LOG = Logger.getLogger(CheckOneDemandMessageDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final TypeRegistry<Chunk.Id<?>> idRegistry;

    private final Set<Class<? extends Chunk>> backupTypes;

    CheckOneDemandMessageDriver(final NetworkWrapper network, final StorageWrapper storage, final TypeRegistry<Chunk.Id<?>> idRegistry,
            final Set<Class<? extends Chunk>> backupTypes) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        } else if (idRegistry == null) {
            throw new IllegalArgumentException("Null id registry.");
        } else if (backupTypes == null) {
            throw new IllegalArgumentException("Null backup types.");
        }
        this.network = network;
        this.storage = storage;
        this.idRegistry = idRegistry;
        this.backupTypes = backupTypes;
    }

    void execute(final CheckOneDemandMessage message, final Session session, final PublicKey sourceId, final InetSocketAddress source)
            throws InterruptedException {
        final List<Message> reply = new ArrayList<>(2);
        if (this.network.moreAppropriate(message.getCandidate().getId().getAddress(), sourceId)) {
            LOG.log(Level.FINEST, "{0} に依頼された {1} を拒否します。", new Object[] { source, message });
            reply.add(CheckOneDemandReply.newRejected());
        } else {
            // データ片の列挙。
            try {
                final DemandEntry demand = DemandEntry.getDemandedEntry(this.storage, message.getCandidate(), this.idRegistry, this.backupTypes);
                if (demand == null) {
                    LOG.log(Level.FINEST, "{0} に依頼された {1} は要りませんでした。", new Object[] { source, message });
                    reply.add(CheckOneDemandReply.newNoDemand());
                } else {
                    LOG.log(Level.FINEST, "{0} に依頼された {1} の需要がありました。", new Object[] { source, message });
                    reply.add(new CheckOneDemandReply(demand));
                }
            } catch (final IOException e) {
                LOG.log(Level.WARNING, "異常が発生しました。", e);
                LOG.log(Level.INFO, "{0} に依頼された {1} を諦めます。", new Object[] { source, message });
                reply.add(CheckOneDemandReply.newGiveUp());
            }
        }
        reply.add(new SessionReply(session));

        // 返信する。
        this.network.sendMail(source, ConnectionTypes.DATA, reply);
        return;
    }
}
