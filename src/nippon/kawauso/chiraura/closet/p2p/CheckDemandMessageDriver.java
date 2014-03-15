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
final class CheckDemandMessageDriver {

    private static final Logger LOG = Logger.getLogger(CheckDemandMessageDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final TypeRegistry<Chunk.Id<?>> idRegistry;

    private final int entryLimit;
    private final Set<Class<? extends Chunk>> backupTypes;

    CheckDemandMessageDriver(final NetworkWrapper network, final StorageWrapper storage, final TypeRegistry<Chunk.Id<?>> idRegistry, final int entryLimit,
            final Set<Class<? extends Chunk>> backupTypes) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        } else if (idRegistry == null) {
            throw new IllegalArgumentException("Null id registry.");
        } else if (entryLimit < 0) {
            throw new IllegalArgumentException("Negative entry limit ( " + entryLimit + " ).");
        } else if (backupTypes == null) {
            throw new IllegalArgumentException("Null backup types.");
        }
        this.network = network;
        this.storage = storage;
        this.idRegistry = idRegistry;

        this.entryLimit = entryLimit;
        this.backupTypes = backupTypes;
    }

    void execute(final CheckDemandMessage message, final Session session, final PublicKey sourceId, final InetSocketAddress source) throws InterruptedException {
        final List<Message> reply = new ArrayList<>(2);
        if (this.network.moreAppropriate(message.getStartAddress(), sourceId) || this.network.moreAppropriate(message.getEndAddress(), sourceId)) {
            LOG.log(Level.FINEST, "{0} に依頼された {1} を拒否します。", new Object[] { source, message });
            reply.add(CheckDemandReply.newRejected());
        } else {
            // データ片の列挙。
            try {
                final List<DemandEntry> entries = DemandEntry.getDemandedEntries(this.storage, message.getStartAddress(), message.getEndAddress(),
                        this.entryLimit, message.getCandidates(), this.idRegistry, this.backupTypes);
                LOG.log(Level.FINEST, "{0} に依頼された {1} への返答に {2} 個発注しました。", new Object[] { source, message, entries.size() });
                reply.add(new CheckDemandReply(entries));
            } catch (final IOException e) {
                LOG.log(Level.WARNING, "異常が発生しました。", e);
                LOG.log(Level.INFO, "{0} に依頼された {1} を諦めます。", new Object[] { source, message });
                reply.add(CheckDemandReply.newGiveUp());
            }
        }
        reply.add(new SessionReply(session));

        // 返信する。
        this.network.sendMail(source, ConnectionTypes.DATA, reply);
        return;
    }
}
