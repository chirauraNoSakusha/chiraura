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
final class GetCacheDriver {

    private static final Logger LOG = Logger.getLogger(GetCacheDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final SessionManager sessionManager;
    private final TypeRegistry<Chunk.Id<?>> idRegistry;
    private final BlockingQueue<OutlawReport> outlawReportSink;

    GetCacheDriver(final NetworkWrapper network, final StorageWrapper storage, final SessionManager sessionManager,
            final TypeRegistry<Chunk.Id<?>> idRegistry, final BlockingQueue<OutlawReport> outlawReportSink) {
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

    boolean isObvious(final GetCacheOperation operation) throws InterruptedException, IOException {
        return this.network.dominates(operation.getId().getAddress()) || this.storage.containsCache(operation.getId());
    }

    GetCacheResult execute(final GetCacheOperation operation, final long timeout) throws InterruptedException, IOException {
        final long start = System.currentTimeMillis();
        final Address address = operation.getId().getAddress();
        final Set<AddressedPeer> usedDestinations = new HashSet<>();

        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < start + timeout) {

            final AddressedPeer destination = this.network.getRoutingDestination(address);

            if (destination == null) {
                LOG.log(Level.FINEST, "{0} の担当は自分でした。", operation.getId());
                final Chunk chunk = this.storage.read(operation.getId());
                if (chunk == null) {
                    LOG.log(Level.FINEST, "{0} はありませんでした。", operation.getId());
                    return GetCacheResult.newNotFound(System.currentTimeMillis());
                } else {
                    LOG.log(Level.FINEST, "{0} がありました。", operation.getId());
                    return new GetCacheResult(chunk, System.currentTimeMillis());
                }
            }

            final StorageWrapper.CacheResult<?> cache = this.storage.readCache(operation.getId());
            if (cache.hasInfo()) {
                LOG.log(Level.FINEST, "ついさっき {0} の情報を更新してました。", operation.getId());
                if (cache.isNotFound()) {
                    LOG.log(Level.FINEST, "{0} は無いらしいです。", operation.getId());
                    return GetCacheResult.newNotFound(cache.getAccessDate());
                } else {
                    LOG.log(Level.FINEST, "{0} の複製がありました。", operation.getId());
                    return new GetCacheResult(cache.getChunk(), cache.getAccessDate());
                }
            }

            if (usedDestinations.contains(destination)) {
                LOG.log(Level.FINEST, "{0} の依頼先 {1} が重複したので諦めます。", new Object[] { operation, destination });
                return GetCacheResult.newGiveUp();
            }
            // やりとりの準備。
            final Session session = this.sessionManager.newSession(destination.getPeer());

            // 手紙の準備。
            final List<Message> mail = new ArrayList<>(2);
            mail.add(new GetCacheMessage(this.idRegistry, operation.getId()));
            mail.add(new SessionMessage(session));

            LOG.log(Level.FINEST, "{0} を {1} に依頼します。", new Object[] { operation, destination });

            // 送受信。
            this.network.sendMail(destination.getPeer(), ConnectionTypes.CONTROL, mail);
            final ReceivedMail receivedMail = this.sessionManager.waitReply(session, start + timeout - System.currentTimeMillis());

            if (receivedMail == null) {
                if (start + timeout <= System.currentTimeMillis()) {
                    LOG.log(Level.FINEST, "{0} は時間切れなので諦めます。", operation);
                    return GetCacheResult.newGiveUp();
                } else {
                    LOG.log(Level.FINEST, "{0} を依頼した {1} との間に何か異常がありました。", new Object[] { operation, destination });
                    usedDestinations.add(destination);
                    continue;
                }
            } else {
                if (receivedMail.getMail().get(0) instanceof GetCacheReply) {
                    // 正常。
                    LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { destination, operation });
                    this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                    final GetCacheReply reply = (GetCacheReply) receivedMail.getMail().get(0);
                    if (reply.isRejected()) {
                        LOG.log(Level.FINEST, "{0} は断られました。", operation);
                        return GetCacheResult.newGiveUp();
                    } else if (reply.isGivenUp()) {
                        LOG.log(Level.FINEST, "{0} は諦められました。", operation);
                        return GetCacheResult.newGiveUp();
                    } else if (reply.isNotFound()) {
                        final StorageWrapper.CacheResult<?> result = this.storage.addNotFoundCache(operation.getId(), reply.getAccessDate());
                        if (result.isNotFound()) {
                            LOG.log(Level.FINEST, "{0} は対象無しと言われました。", operation);
                            return GetCacheResult.newNotFound(reply.getAccessDate());
                        } else {
                            LOG.log(Level.FINEST, "{0} は対象無しと言われたけど複製がありました。", operation);
                            return new GetCacheResult(result.getChunk(), result.getAccessDate());
                        }
                    } else {
                        final StorageWrapper.CacheResult<?> result = this.storage.forceWriteCache(reply.getChunk(), reply.getAccessDate());
                        if (result.isSuccess()) {
                            LOG.log(Level.FINEST, "{0} が成功し更新されました。", operation);
                        } else {
                            LOG.log(Level.FINEST, "{0} が成功したけど更新はされませんでした。", operation);
                        }
                        return new GetCacheResult(result.getChunk(), result.getAccessDate());
                    }
                } else {
                    // プロトコル違反。
                    LOG.log(Level.WARNING, "{0} からの返事の型 {1} は期待する型 {2} と異なります。", new Object[] { destination, receivedMail.getMail().get(0).getClass(),
                            GetCacheReply.class });
                    ConcurrentFunctions.completePut(new OutlawReport(destination.getPeer()), this.outlawReportSink);
                    continue;
                }
            }
        }

        return GetCacheResult.newGiveUp();
    }

}
