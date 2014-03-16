package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.messenger.ReceivedMail;
import nippon.kawauso.chiraura.network.AddressedPeer;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class GetChunkDriver {

    private static final Logger LOG = Logger.getLogger(GetChunkDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final SessionManager sessionManager;
    private final TypeRegistry<Chunk.Id<?>> idRegistry;
    private final BlockingQueue<OutlawReport> outlawReportSink;

    GetChunkDriver(final NetworkWrapper network, final StorageWrapper storage, final SessionManager sessionManager, final TypeRegistry<Chunk.Id<?>> idRegistry,
            final BlockingQueue<OutlawReport> outlawReportSink) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        } else if (sessionManager == null) {
            throw new IllegalArgumentException("Null session manager.");
        } else if (idRegistry == null) {
            throw new IllegalArgumentException("Null id registry.");
        } else if (outlawReportSink == null) {
            throw new IllegalArgumentException("Null outlaw report sink.");
        }
        this.network = network;
        this.storage = storage;
        this.sessionManager = sessionManager;
        this.idRegistry = idRegistry;
        this.outlawReportSink = outlawReportSink;
    }

    boolean isObvious(final GetChunkOperation operation) {
        return this.network.dominates(operation.getId().getAddress());
    }

    GetChunkResult execute(final GetChunkOperation operation, final long timeout) throws InterruptedException, IOException {
        final long start = System.currentTimeMillis();
        final Address address = operation.getId().getAddress();
        final Set<AddressedPeer> usedDestinations = new HashSet<>();

        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < start + timeout) {

            final AddressedPeer destination = this.network.getRoutingDestination(address);

            if (destination == null) {
                // 自分の領土 (になった)。
                LOG.log(Level.FINEST, "{0} の担当は自分でした。", operation);
                final Chunk chunk = this.storage.read(operation.getId());
                if (chunk == null) {
                    LOG.log(Level.FINEST, "{0} はありませんでした。", operation.getId());
                    return GetChunkResult.newNotFound();
                } else {
                    LOG.log(Level.FINEST, "{0} がありました。", operation.getId());
                    return new GetChunkResult(chunk);
                }
            }

            if (usedDestinations.contains(destination)) {
                LOG.log(Level.FINEST, "{0} の依頼先 {1} が重複したので諦めます。", new Object[] { operation, destination });
                return GetChunkResult.newGiveUp();
            }

            // やりとりの準備。
            final Session session = this.sessionManager.newSession(destination.getPeer());

            // 手紙の準備。
            final List<Message> mail = new ArrayList<>(2);
            mail.add(new GetChunkMessage(this.idRegistry, operation.getId()));
            mail.add(new SessionMessage(session));

            LOG.log(Level.FINEST, "{0} を {1} に依頼します。", new Object[] { operation, destination });

            // 送受信。
            this.network.sendMail(destination.getPeer(), ConnectionTypes.CONTROL, mail);
            final ReceivedMail receivedMail = this.sessionManager.waitReply(session, start + timeout - System.currentTimeMillis());

            if (receivedMail == null) {
                if (start + timeout <= System.currentTimeMillis()) {
                    LOG.log(Level.FINEST, "{0} は時間切れなので諦めます。", operation);
                    return GetChunkResult.newGiveUp();
                } else {
                    LOG.log(Level.FINEST, "{0} を依頼した {1} との間に何か異常がありました。", new Object[] { operation, destination });
                    usedDestinations.add(destination);
                    continue;
                }
            } else {
                if (receivedMail.getMail().get(0) instanceof GetChunkReply) {
                    // 正常。
                    LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { destination, operation });
                    this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                    final GetChunkReply reply = (GetChunkReply) receivedMail.getMail().get(0);
                    if (reply.isRejected()) {
                        LOG.log(Level.FINEST, "{0} は断られました。", operation);
                        return GetChunkResult.newGiveUp();
                    } else if (reply.isGivenUp()) {
                        LOG.log(Level.FINEST, "{0} は諦められました。", operation);
                        return GetChunkResult.newGiveUp();
                    } else if (reply.isNotFound()) {
                        LOG.log(Level.FINEST, "{0} は対象無しと言われました。", operation);
                        return GetChunkResult.newNotFound();
                    } else {
                        LOG.log(Level.FINEST, "{0} が成功しました。", operation);
                        return new GetChunkResult(reply.getChunk());
                    }
                } else {
                    // プロトコル違反。
                    LOG.log(Level.WARNING, "{0} からの返事の型 {1} は期待する型 {2} と異なります。", new Object[] { destination, receivedMail.getMail().get(0).getClass(),
                            GetChunkReply.class });
                    ConcurrentFunctions.completePut(new OutlawReport(destination.getPeer()), this.outlawReportSink);
                    continue;
                }
            }
        }

        return GetChunkResult.newGiveUp();
    }
}
