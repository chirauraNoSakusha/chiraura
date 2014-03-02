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

    CheckDemandMessageDriver(final NetworkWrapper network, final StorageWrapper storage, final TypeRegistry<Chunk.Id<?>> idRegistry, final int entryLimit) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        } else if (idRegistry == null) {
            throw new IllegalArgumentException("Null id registry.");
        } else if (entryLimit < 0) {
            throw new IllegalArgumentException("Negative entry limit ( " + entryLimit + " ).");
        }
        this.network = network;
        this.storage = storage;
        this.idRegistry = idRegistry;

        this.entryLimit = entryLimit;
    }

    void execute(final CheckDemandMessage message, final Session session, final PublicKey sourceId, final InetSocketAddress source) throws InterruptedException {
        final List<Message> reply = new ArrayList<>(2);
        if (this.network.moreAppropriate(message.getStartAddress(), sourceId) || this.network.moreAppropriate(message.getEndAddress(), sourceId)) {
            LOG.log(Level.FINEST, "{0} に依頼された {1} を拒否します。", new Object[] { source, message });
            reply.add(CheckDemandReply.newRejected());
        } else {
            // データ片の列挙。
            List<DemandEntry> entries = null;
            try {
                entries = DemandEntry.getDemandedEntries(this.storage, message.getStartAddress(), message.getEndAddress(), this.entryLimit,
                        message.getCandidates(), this.idRegistry);
            } catch (final IOException e) {
                LOG.log(Level.WARNING, source + " に依頼された " + message + " への応答中に異常が発生しました。", e);
            }

            if (entries == null) {
                reply.add(CheckDemandReply.newGiveUp());
            } else {
                LOG.log(Level.FINEST, "{0} に依頼された {1} への返答に {2} 個発注しました。", new Object[] { source, message, entries.size() });
                reply.add(new CheckDemandReply(entries));
            }
        }
        reply.add(new SessionReply(session));

        // 返信する。
        this.network.sendMail(source, ConnectionTypes.DATA, reply);
        return;
    }
}
