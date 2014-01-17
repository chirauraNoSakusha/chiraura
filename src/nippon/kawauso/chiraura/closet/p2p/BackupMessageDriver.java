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

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class BackupMessageDriver {

    private static final Logger LOG = Logger.getLogger(BackupMessageDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final TypeRegistry<Chunk> chunkRegistry;

    BackupMessageDriver(final NetworkWrapper network, final StorageWrapper storage, final TypeRegistry<Chunk> chunkRegistry) {
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

    <T extends Mountain> void execute(final BackupMessage<T> message, final Session session, final PublicKey sourceId, final InetSocketAddress source)
            throws InterruptedException {

        final List<Message> reply = new ArrayList<>(2);
        if (message.isGet()) {
            final Address address = message.getChunk().getId().getAddress();
            if (this.network.moreAppropriate(address, sourceId)) {
                LOG.log(Level.FINEST, "{0} に依頼された {1} を拒否します。", new Object[] { source, message });
                reply.add(BackupReply.newRejected());
            } else {
                StorageWrapper.Result<?> result = null;
                try {
                    // System.out.println("Aho " + message.getChunk());
                    result = this.storage.forceUpdate(message.getChunk());
                } catch (final IOException e) {
                    LOG.log(Level.WARNING, message.getChunk().getId() + " の書き込み中に異常が発生しました。", e);
                }
                if (result == null) {
                    reply.add(BackupReply.newGiveUp());
                } else if (result.getChunk().equals(message.getChunk())) {
                    LOG.log(Level.FINEST, "{0} に依頼された {1} が成功しました。", new Object[] { source, message });
                    reply.add(new BackupReply());
                } else {
                    LOG.log(Level.FINEST, "{0} に依頼された {1} は不完全です。", new Object[] { source, message });
                    reply.add(BackupReply.newFailure(this.chunkRegistry, result.getChunk()));
                }
            }
        } else {
            final Address address = message.getId().getAddress();
            if (this.network.moreAppropriate(address, sourceId)) {
                LOG.log(Level.FINEST, "{0} に依頼された {1} を拒否します。", new Object[] { source, message });
                reply.add(BackupReply.newRejected());
            } else {
                StorageWrapper.Result<T> result = null;
                try {
                    result = this.storage.patch(message.getId(), message.getDiffs());
                } catch (final IOException e) {
                    LOG.log(Level.WARNING, message.getId() + " の書き込み中に異常が発生しました。", e);
                }
                if (result == null || result.isNotFound()) {
                    reply.add(BackupReply.newGiveUp());
                } else if (result.getChunk().getHashValue().equals(message.getHashValue())) {
                    LOG.log(Level.FINEST, "{0} に依頼された {1} が成功しました。", new Object[] { source, message });
                    reply.add(new BackupReply());
                } else {
                    LOG.log(Level.FINEST, "{0} に依頼された {1} は不完全です。", new Object[] { source, message });
                    reply.add(BackupReply.newFailure(this.chunkRegistry, result.getChunk()));
                }
            }
        }
        reply.add(new SessionReply(session));

        // 返信する。
        this.network.sendMail(source, ConnectionTypes.CONTROL, reply);
        return;
    }

}
