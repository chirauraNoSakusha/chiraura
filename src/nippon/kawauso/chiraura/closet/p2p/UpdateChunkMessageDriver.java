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

/**
 * @author chirauraNoSakusha
 */
final class UpdateChunkMessageDriver {

    private static final Logger LOG = Logger.getLogger(UpdateChunkMessageDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final UpdateChunkBlockingDriver blockingDriver;
    private final TypeRegistry<Mountain.Dust<?>> diffRegistry;
    private final ExecutorService executor;

    UpdateChunkMessageDriver(final NetworkWrapper network, final UpdateChunkBlockingDriver blockingDriver, final TypeRegistry<Mountain.Dust<?>> diffRegistry,
            final ExecutorService executor) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (blockingDriver == null) {
            throw new IllegalArgumentException("Null blocking driver.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        } else if (diffRegistry == null) {
            throw new IllegalArgumentException("Null diff registry.");
        }
        this.network = network;
        this.blockingDriver = blockingDriver;
        this.diffRegistry = diffRegistry;
        this.executor = executor;
    }

    void execute(final UpdateChunkMessage message, final Session session, final PublicKey sourceId, final InetSocketAddress source, final long timeout) {

        if (!this.network.moreAppropriate(message.getId().getAddress(), sourceId)) {
            LOG.log(Level.FINEST, "{0} に依頼された {1} を拒否します。", new Object[] { source, message });
            final List<Message> reply = new ArrayList<>(2);
            reply.add(UpdateChunkReply.newRejected());
            reply.add(new SessionReply(session));
            this.network.sendMail(source, ConnectionTypes.CONTROL, reply);
            return;
        }

        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws Exception {
                final UpdateChunkOperation operation = new UpdateChunkOperation(message.getId(), message.getDate());
                final UpdateChunkResult result = UpdateChunkMessageDriver.this.blockingDriver.execute(operation, timeout);

                // 返信の用意。
                final List<Message> reply = new ArrayList<>(2);
                final int connectionType;
                if (result == null) {
                    // 制限時間中に結果を得られなかった。(やったのは先人かも)
                    reply.add(UpdateChunkReply.newGiveUp());
                    connectionType = ConnectionTypes.CONTROL;
                } else if (result.isGivenUp()) {
                    // 通信先が諦めた。俺も諦める。
                    reply.add(UpdateChunkReply.newGiveUp());
                    connectionType = ConnectionTypes.CONTROL;
                } else if (result.isNotFound()) {
                    reply.add(UpdateChunkReply.newNotFound());
                    connectionType = ConnectionTypes.CONTROL;
                } else {
                    reply.add(new UpdateChunkReply(UpdateChunkMessageDriver.this.diffRegistry, result.getDiffs()));
                    connectionType = ConnectionTypes.DATA;
                }
                reply.add(new SessionReply(session));

                // 返信する。
                UpdateChunkMessageDriver.this.network.sendMail(source, connectionType, reply);
                LOG.log(Level.FINEST, "{0} からの {1} に応えました。", new Object[] { source, message });
                return null;
            }
        });
    }

}
