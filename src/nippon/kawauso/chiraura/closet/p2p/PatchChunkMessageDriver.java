package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;

/**
 * @author chirauraNoSakusha
 */
final class PatchChunkMessageDriver {

    private static final Logger LOG = Logger.getLogger(PatchChunkMessageDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final PatchChunkBlockingDriver blockingDriver;
    private final ExecutorService executor;

    PatchChunkMessageDriver(final NetworkWrapper network, final PatchChunkBlockingDriver blockingDriver, final ExecutorService executor) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (blockingDriver == null) {
            throw new IllegalArgumentException("Null blocking driver.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        }
        this.network = network;
        this.blockingDriver = blockingDriver;
        this.executor = executor;
    }

    <T extends Mountain> void execute(final PatchChunkMessage<T> message, final Session session, final PublicKey sourceId, final InetSocketAddress source,
            final long timeout) {

        if (!this.network.moreAppropriate(message.getId().getAddress(), sourceId)) {
            LOG.log(Level.FINEST, "{0} に依頼された {1} を拒否します。", new Object[] { source, message });
            final List<Message> reply = new ArrayList<>(2);
            reply.add(PatchChunkReply.newRejected());
            reply.add(new SessionReply(session));
            this.network.sendMail(source, ConnectionTypes.CONTROL, reply);
            return;
        }

        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws InterruptedException, IOException {
                final PatchChunkOperation<T> operation = new PatchChunkOperation<>(message.getId(), message.getDiff());
                final PatchChunkResult result = PatchChunkMessageDriver.this.blockingDriver.execute(operation, timeout);

                // 返信の用意。
                final List<Message> reply = new ArrayList<>(2);
                if (result == null) {
                    // 制限時間中に結果を得られなかった。(やったのは先人かも)
                    reply.add(PatchChunkReply.newGiveUp());
                } else if (result.isGivenUp()) {
                    // 通信先が諦めた。俺も諦める。
                    reply.add(PatchChunkReply.newGiveUp());
                } else if (result.isNotFound()) {
                    reply.add(PatchChunkReply.newNotFound());
                } else if (result.isSuccess()) {
                    reply.add(new PatchChunkReply());
                } else {
                    reply.add(PatchChunkReply.newFailure());
                }
                reply.add(new SessionReply(session));

                // 返信する。
                PatchChunkMessageDriver.this.network.sendMail(source, ConnectionTypes.CONTROL, reply);
                LOG.log(Level.FINEST, "{0} からの {1} に応えました。", new Object[] { source, message });
                return null;
            }
        });
    }

}
