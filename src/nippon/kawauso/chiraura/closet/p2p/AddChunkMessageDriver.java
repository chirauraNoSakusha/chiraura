package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;

/**
 * @author chirauraNoSakusha
 */
final class AddChunkMessageDriver {

    private static final Logger LOG = Logger.getLogger(AddChunkMessageDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final AddChunkBlockingDriver blockingDriver;
    private final ExecutorService executor;

    AddChunkMessageDriver(final NetworkWrapper network, final AddChunkBlockingDriver blockingDriver, final ExecutorService executor) {
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

    void execute(final AddChunkMessage message, final Session session, final PublicKey sourceId, final InetSocketAddress source, final long timeout) {

        if (!this.network.moreAppropriate(message.getChunk().getId().getAddress(), sourceId)) {
            LOG.log(Level.FINEST, "{0} に依頼された {1} を拒否します。", new Object[] { source, message });
            final List<Message> reply = new ArrayList<>(2);
            reply.add(AddChunkReply.newRejected());
            reply.add(new SessionReply(session));
            this.network.sendMail(source, ConnectionTypes.CONTROL, reply);
            return;
        }

        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws IOException, InterruptedException {
                final AddChunkOperation operation = new AddChunkOperation(message.getChunk());
                final AddChunkResult result = AddChunkMessageDriver.this.blockingDriver.execute(operation, timeout);

                // 返信の用意。
                final List<Message> reply = new ArrayList<>(2);
                if (result == null) {
                    // 制限時間中に結果を得られなかった。(やったのは先人かも)
                    reply.add(AddChunkReply.newGiveUp());
                } else if (result.isGivenUp()) {
                    reply.add(AddChunkReply.newGiveUp());
                } else if (result.isSuccess()) {
                    reply.add(new AddChunkReply());
                } else {
                    reply.add(AddChunkReply.newFailure());
                }
                reply.add(new SessionReply(session));

                // 返信する。
                AddChunkMessageDriver.this.network.sendMail(source, ConnectionTypes.CONTROL, reply);
                LOG.log(Level.FINEST, "{0} からの {1} に応えました。", new Object[] { source, message });

                return null;
            }
        });
    }
}
