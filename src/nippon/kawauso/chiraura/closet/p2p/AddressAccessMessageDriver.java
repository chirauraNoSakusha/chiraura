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
 * 論理位置の支配者確認のための言付けを捌く。
 * @author chirauraNoSakusha
 */
final class AddressAccessMessageDriver {

    private static final Logger LOG = Logger.getLogger(AddressAccessMessageDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final AddressAccessBlockingDriver blockingDriver;
    private final ExecutorService executor;

    AddressAccessMessageDriver(final NetworkWrapper network, final AddressAccessBlockingDriver blockingDriver, final ExecutorService executor) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (blockingDriver == null) {
            throw new IllegalArgumentException("Null blocking driver.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        }

        this.network = network;
        this.executor = executor;
        this.blockingDriver = blockingDriver;
    }

    void execute(final AddressAccessMessage message, final Session session, final PublicKey sourceId, final InetSocketAddress source, final long timeout) {

        if (!this.network.moreAppropriate(message.getAddress(), sourceId)) {
            LOG.log(Level.FINEST, "{0} に依頼された {1} を拒否します。", new Object[] { source, message });
            final List<Message> reply = new ArrayList<>(2);
            reply.add(AddressAccessReply.newRejected());
            reply.add(new SessionReply(session));
            this.network.sendMail(source, ConnectionTypes.CONTROL, reply);
            return;
        }

        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            public Void subCall() throws InterruptedException {
                final AddressAccessOperation operation = new AddressAccessOperation(message.getAddress());

                final AddressAccessResult result = AddressAccessMessageDriver.this.blockingDriver.execute(operation, timeout);

                // 返信の用意。
                final List<Message> reply = new ArrayList<>(2);
                if (result == null) {
                    // 制限時間中に結果を得られなかった。(やったのは先人かも)
                    reply.add(AddressAccessReply.newGiveUp());
                } else if (result.isGivenUp()) {
                    reply.add(AddressAccessReply.newGiveUp());
                } else {
                    reply.add(new AddressAccessReply(result.getManager(), result.getPeers()));
                }
                reply.add(new SessionReply(session));

                // 返信する。
                AddressAccessMessageDriver.this.network.sendMail(source, ConnectionTypes.CONTROL, reply);
                LOG.log(Level.FINEST, "{0} からの {1} に応えました。", new Object[] { source, message });

                return null;
            }
        });
    }
}
