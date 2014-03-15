package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class PatchOrAddAndGetCacheMessageDriver {

    private static final Logger LOG = Logger.getLogger(PatchOrAddAndGetCacheMessageDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final PatchOrAddAndGetCacheBlockingDriver blockingDriver;
    private final TypeRegistry<Chunk> chunkRegistry;
    private final ExecutorService executor;

    PatchOrAddAndGetCacheMessageDriver(final NetworkWrapper network, final PatchOrAddAndGetCacheBlockingDriver blockingDriver,
            final TypeRegistry<Chunk> chunkRegistry, final ExecutorService executor) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (blockingDriver == null) {
            throw new IllegalArgumentException("Null blocking driver.");
        } else if (chunkRegistry == null) {
            throw new IllegalArgumentException("Null chunk registry.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        }
        this.network = network;
        this.blockingDriver = blockingDriver;
        this.chunkRegistry = chunkRegistry;
        this.executor = executor;
    }

    void execute(final PatchOrAddAndGetCacheMessage message, final Session session, final PublicKey sourceId, final InetSocketAddress source, final long timeout) {

        if (!this.network.moreAppropriate(message.getChunk().getId().getAddress(), sourceId)) {
            LOG.log(Level.FINEST, "{0} に依頼された {1} を拒否します。", new Object[] { source, message });
            final List<Message> reply = new ArrayList<>(2);
            reply.add(PatchOrAddAndGetCacheReply.newRejected());
            reply.add(new SessionReply(session));
            this.network.sendMail(source, ConnectionTypes.CONTROL, reply);
            return;
        }

        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws Exception {
                final PatchOrAddAndGetCacheOperation operation = new PatchOrAddAndGetCacheOperation(message.getChunk());
                final PatchOrAddAndGetCacheResult result = PatchOrAddAndGetCacheMessageDriver.this.blockingDriver.execute(operation, timeout);

                // 返信の用意。
                final List<Message> reply = new ArrayList<>(2);
                final int connectionType;
                if (result == null) {
                    // 制限時間中に結果を得られなかった。(やったのは先人かも)
                    reply.add(PatchOrAddAndGetCacheReply.newGiveUp());
                    connectionType = ConnectionTypes.CONTROL;
                } else if (result.isGivenUp()) {
                    // 通信先が諦めた。俺も諦める。
                    reply.add(PatchOrAddAndGetCacheReply.newGiveUp());
                    connectionType = ConnectionTypes.CONTROL;
                } else {
                    reply.add(new PatchOrAddAndGetCacheReply(PatchOrAddAndGetCacheMessageDriver.this.chunkRegistry, result.getChunk(), result.getAccessDate()));
                    connectionType = ConnectionTypes.DATA;
                }
                reply.add(new SessionReply(session));

                // 返信する。
                PatchOrAddAndGetCacheMessageDriver.this.network.sendMail(source, connectionType, reply);
                LOG.log(Level.FINEST, "{0} からの {1} に応えました。", new Object[] { source, message });
                return null;
            }
        });
    }

}
