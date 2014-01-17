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

import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;

/**
 * @author chirauraNoSakusha
 */
final class AddCacheMessageDriver {

    private static final Logger LOG = Logger.getLogger(AddCacheMessageDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final AddCacheBlockingDriver blockingDriver;
    private final ExecutorService executor;

    AddCacheMessageDriver(final NetworkWrapper network, final AddCacheBlockingDriver blockingDriver, final ExecutorService executor) {
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

    void execute(final AddCacheMessage message, final Session session, final PublicKey sourceId, final InetSocketAddress source, final long timeout) {

        if (!this.network.moreAppropriate(message.getChunk().getId().getAddress(), sourceId)) {
            LOG.log(Level.FINEST, "{0} に依頼された {1} を拒否します。", new Object[] { source, message });
            final List<Message> reply = new ArrayList<>(2);
            reply.add(AddCacheReply.newRejected());
            reply.add(new SessionReply(session));
            this.network.sendMail(source, ConnectionTypes.CONTROL, reply);
            return;
        }

        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws Exception {
                final AddCacheOperation operation = new AddCacheOperation(message.getChunk());
                final AddCacheResult result = AddCacheMessageDriver.this.blockingDriver.execute(operation, timeout);

                // 返信の用意。
                final List<Message> reply = new ArrayList<>(2);
                if (result == null) {
                    // 時間切れ。
                    reply.add(AddCacheReply.newGiveUp());
                } else if (result.isGivenUp()) {
                    reply.add(AddCacheReply.newGiveUp());
                } else if (result.isSuccess()) {
                    reply.add(new AddCacheReply(result.getAccessDate()));
                } else {
                    reply.add(AddCacheReply.newFailure());
                }
                reply.add(new SessionReply(session));

                // 返信する。
                AddCacheMessageDriver.this.network.sendMail(source, ConnectionTypes.CONTROL, reply);

                LOG.log(Level.FINEST, "{0} からの {1} に応えました。", new Object[] { source, message });
                return null;
            }
        });
    }
}
