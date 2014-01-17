/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class PatchAndGetOrUpdateCacheMessageDriver {

    private static final Logger LOG = Logger.getLogger(PatchAndGetOrUpdateCacheMessageDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final PatchAndGetOrUpdateCacheBlockingDriver blockingDriver;
    private final TypeRegistry<Chunk> chunkRegistry;
    private final TypeRegistry<Chunk.Id<?>> idRegistry;
    private final ExecutorService executor;

    PatchAndGetOrUpdateCacheMessageDriver(final NetworkWrapper network, final PatchAndGetOrUpdateCacheBlockingDriver blockingDriver,
            final TypeRegistry<Chunk> chunkRegistry, final TypeRegistry<Chunk.Id<?>> idRegistry, final ExecutorService executor) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (blockingDriver == null) {
            throw new IllegalArgumentException("Null blocking driver.");
        } else if (chunkRegistry == null) {
            throw new IllegalArgumentException("Null chunk registry.");
        } else if (idRegistry == null) {
            throw new IllegalArgumentException("Null id registry.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        }
        this.network = network;
        this.blockingDriver = blockingDriver;
        this.chunkRegistry = chunkRegistry;
        this.idRegistry = idRegistry;
        this.executor = executor;
    }

    <T extends Mountain> void execute(final PatchAndGetOrUpdateCacheMessage<T> message, final Session session, final PublicKey sourceId,
            final InetSocketAddress source,
            final long timeout) {

        if (!this.network.moreAppropriate(message.getId().getAddress(), sourceId)) {
            LOG.log(Level.FINEST, "{0} に依頼された {1} を拒否します。", new Object[] { source, message });
            final List<Message> reply = new ArrayList<>(2);
            reply.add(PatchAndGetOrUpdateCacheReply.newRejected());
            reply.add(new SessionReply(session));
            this.network.sendMail(source, ConnectionTypes.CONTROL, reply);
            return;
        }

        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws Exception {
                final PatchAndGetOrUpdateCacheOperation<T> operation = new PatchAndGetOrUpdateCacheOperation<>(message.getId(), message.getDiff());
                final PatchAndGetOrUpdateCacheResult result = PatchAndGetOrUpdateCacheMessageDriver.this.blockingDriver.execute(operation, timeout);

                // 返信の用意。
                final List<Message> reply = new ArrayList<>(2);
                final int connectionType;
                if (result == null) {
                    // 制限時間中に結果を得られなかった。(やったのは先人かも)
                    reply.add(PatchAndGetOrUpdateCacheReply.newGiveUp());
                    connectionType = ConnectionTypes.CONTROL;
                } else if (result.isGivenUp()) {
                    // 通信先が諦めた。俺も諦める。
                    reply.add(PatchAndGetOrUpdateCacheReply.newGiveUp());
                    connectionType = ConnectionTypes.CONTROL;
                } else if (result.isNotFound()) {
                    reply.add(PatchAndGetOrUpdateCacheReply.newNotFound(PatchAndGetOrUpdateCacheMessageDriver.this.idRegistry, message.getId(),
                            result.getAccessDate()));
                    connectionType = ConnectionTypes.CONTROL;
                } else {
                    if (message.isGet()) {
                        reply.add(new PatchAndGetOrUpdateCacheReply<>(result.isSuccess(), PatchAndGetOrUpdateCacheMessageDriver.this.chunkRegistry,
                                result.getChunk(), result.getAccessDate()));
                    } else {
                        reply.add(new PatchAndGetOrUpdateCacheReply<>(result.isSuccess(), PatchAndGetOrUpdateCacheMessageDriver.this.chunkRegistry, result
                                .getChunk(), message.getDate(), result.getAccessDate()));
                    }
                    connectionType = ConnectionTypes.DATA;
                }
                reply.add(new SessionReply(session));

                // 返信する。
                PatchAndGetOrUpdateCacheMessageDriver.this.network.sendMail(source, connectionType, reply);
                LOG.log(Level.FINEST, "{0} からの {1} に応えました。", new Object[] { source, message });
                return null;
            }

        });
    }

}
