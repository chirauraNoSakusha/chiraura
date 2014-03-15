package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.messenger.ReceivedMail;
import nippon.kawauso.chiraura.network.AddressedPeer;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class GetOrUpdateCacheDriver {

    private static final Logger LOG = Logger.getLogger(GetOrUpdateCacheDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final SessionManager sessionManager;
    private final TypeRegistry<Chunk.Id<?>> idRegistry;

    private final GetCacheBlockingDriver getDriver;
    private final PatchOrAddAndGetCacheBlockingDriver patchAndGetDriver;

    GetOrUpdateCacheDriver(final NetworkWrapper network, final StorageWrapper storage, final SessionManager sessionManager,
            final TypeRegistry<Chunk.Id<?>> idRegistry, final GetCacheBlockingDriver getDriver, final PatchOrAddAndGetCacheBlockingDriver patchAndGetDriver) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        } else if (sessionManager == null) {
            throw new IllegalArgumentException("Null session manager.");
        } else if (idRegistry == null) {
            throw new IllegalArgumentException("Null id registry.");
        } else if (getDriver == null) {
            throw new IllegalArgumentException("Null get driver.");
        } else if (patchAndGetDriver == null) {
            throw new IllegalArgumentException("Null patch and get driver.");
        }
        this.network = network;
        this.storage = storage;
        this.sessionManager = sessionManager;
        this.idRegistry = idRegistry;
        this.getDriver = getDriver;
        this.patchAndGetDriver = patchAndGetDriver;
    }

    boolean isObvious(final GetOrUpdateCacheOperation operation) throws InterruptedException, IOException {
        return this.network.dominates(operation.getId().getAddress()) || this.storage.containsCache(operation.getId());
    }

    GetOrUpdateCacheResult execute(final GetOrUpdateCacheOperation operation, final long timeout) throws InterruptedException, IOException {
        final long start = System.currentTimeMillis();
        final Address address = operation.getId().getAddress();
        final Set<AddressedPeer> usedDestinations = new HashSet<>();

        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < start + timeout) {

            final AddressedPeer destination = this.network.getRoutingDestination(address);

            if (destination == null) {
                LOG.log(Level.FINEST, "{0} の担当は自分でした。", operation.getId());
                final Mountain chunk = this.storage.read(operation.getId());
                if (chunk == null) {
                    LOG.log(Level.FINEST, "{0} はありませんでした。", operation.getId());
                    return GetOrUpdateCacheResult.newNotFound(System.currentTimeMillis());
                } else {
                    LOG.log(Level.FINEST, "{0} がありました。", operation.getId());
                    return new GetOrUpdateCacheResult(chunk, System.currentTimeMillis());
                }
            }

            final StorageWrapper.CacheResult<? extends Mountain> cache = this.storage.readCache(operation.getId());
            if (cache.hasInfo()) {
                LOG.log(Level.FINEST, "ついさっき {0} の情報を更新してました。", operation.getId());
                if (cache.isNotFound()) {
                    LOG.log(Level.FINEST, "{0} は無いらしいです。", operation.getId());
                    return GetOrUpdateCacheResult.newNotFound(cache.getAccessDate());
                } else {
                    LOG.log(Level.FINEST, "{0} の複製がありました。", operation.getId());
                    return new GetOrUpdateCacheResult(cache.getChunk(), cache.getAccessDate());
                }
            }

            if (usedDestinations.contains(destination)) {
                LOG.log(Level.FINEST, "{0} の依頼先 {1} が重複したので諦めます。", new Object[] { operation, destination });
                return GetOrUpdateCacheResult.newGiveUp();
            }

            // やりとりの準備。
            final Session session = this.sessionManager.newSession(destination.getPeer());

            // 手紙の準備。
            final List<Message> mail = new ArrayList<>(2);
            final Mountain chunk = this.storage.read(operation.getId());
            if (chunk == null) {
                mail.add(new GetOrUpdateCacheMessage(this.idRegistry, operation.getId()));
            } else {
                mail.add(new GetOrUpdateCacheMessage(this.idRegistry, operation.getId(), chunk.getDate()));
            }
            mail.add(new SessionMessage(session));

            LOG.log(Level.FINEST, "{0} を {1} に依頼します。", new Object[] { operation, destination });

            // 送受信。
            this.network.sendMail(destination.getPeer(), ConnectionTypes.CONTROL, mail);
            final ReceivedMail receivedMail = this.sessionManager.waitReply(session, start + timeout - System.currentTimeMillis());

            if (receivedMail == null) {
                if (start + timeout <= System.currentTimeMillis()) {
                    LOG.log(Level.FINEST, "{0} は時間切れなので諦めます。", operation);
                    return GetOrUpdateCacheResult.newGiveUp();
                } else {
                    LOG.log(Level.FINEST, "{0} を依頼した {1} との間に何か異常がありました。", new Object[] { operation, destination });
                    usedDestinations.add(destination);
                    continue;
                }
            } else {
                if (receivedMail.getMail().get(0) instanceof GetOrUpdateCacheReply) {
                    // 正常。
                    LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { destination, operation });
                    this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                    final GetOrUpdateCacheReply<?> reply = (GetOrUpdateCacheReply<?>) receivedMail.getMail().get(0);
                    if (reply.isRejected()) {
                        LOG.log(Level.FINEST, "{0} は断られました。", operation);
                        return GetOrUpdateCacheResult.newGiveUp();
                    } else if (reply.isGivenUp()) {
                        LOG.log(Level.FINEST, "{0} は諦められました。", operation);
                        return GetOrUpdateCacheResult.newGiveUp();
                    } else if (reply.isNotFound()) {
                        final StorageWrapper.CacheResult<? extends Mountain> cache0 = this.storage.addNotFoundCache(operation.getId(), reply.getAccessDate());
                        if (cache0.isNotFound()) {
                            LOG.log(Level.FINEST, "{0} は対象無しと言われました。", operation);
                            return GetOrUpdateCacheResult.newNotFound(reply.getAccessDate());
                        } else {
                            LOG.log(Level.FINEST, "{0} は対象無しと言われたけど複製がありました。", operation);
                            return new GetOrUpdateCacheResult(cache0.getChunk(), cache0.getAccessDate());
                        }
                    } else if (reply.isGet()) {
                        final StorageWrapper.CacheResult<? extends Mountain> result = this.storage.forceWriteCache(reply.getChunk(), reply.getAccessDate());
                        if (result.isSuccess()) {
                            LOG.log(Level.FINEST, "{0} が成功し更新されました。", operation);
                        } else {
                            LOG.log(Level.FINEST, "{0} が成功したけど更新はされませんでした。", operation);
                        }
                        return new GetOrUpdateCacheResult(result.getChunk(), result.getAccessDate());
                    } else {
                        final Mountain before = this.storage.read(operation.getId());
                        if (before == null) {
                            LOG.log(Level.FINEST, "{0} の応答として差分が来たけど基礎を持ってなかったので一から取り寄せます。", operation);
                            final GetCacheOperation subOperation = new GetCacheOperation(operation.getId());
                            final GetCacheResult subResult = this.getDriver.execute(subOperation, start + timeout - System.currentTimeMillis());
                            if (subResult.isGivenUp()) {
                                LOG.log(Level.FINEST, "{0} は一悶着の末に諦められました。", operation);
                                return GetOrUpdateCacheResult.newGiveUp();
                            } else if (subResult.isNotFound()) {
                                LOG.log(Level.FINEST, "{0} は一悶着の末に対象無しと言われました。", operation);
                                return GetOrUpdateCacheResult.newNotFound(subResult.getAccessDate());
                            } else {
                                LOG.log(Level.FINEST, "{0} が一悶着の末に成功しました。", operation);
                                return new GetOrUpdateCacheResult((Mountain) subResult.getChunk(), subResult.getAccessDate());
                            }
                        } else {
                            final Mountain after = before.copy();
                            for (final Mountain.Dust<?> diff : reply.getDiffs()) {
                                after.patch(diff);
                            }
                            if (after.getHashValue().equals(reply.getHashValue())) {
                                LOG.log(Level.FINEST, "{0} が成功しました。", operation);
                                this.storage.forceWriteCache(after, reply.getAccessDate());
                                return new GetOrUpdateCacheResult(after, reply.getAccessDate());
                            } else {
                                LOG.log(Level.FINEST, "{0} の応答として来た差分だけでは同期できなかったので、もいっちょ頑張ります。", operation);
                                final PatchOrAddAndGetCacheResult result = this.patchAndGetDriver.execute(new PatchOrAddAndGetCacheOperation(before),
                                        start + timeout - System.currentTimeMillis());
                                if (result.isGivenUp()) {
                                    LOG.log(Level.FINEST, "{0} は一悶着の末に諦められました。", operation);
                                    return GetOrUpdateCacheResult.newGiveUp();
                                } else {
                                    LOG.log(Level.FINEST, "{0} が一悶着の末に成功しました。", operation);
                                    return new GetOrUpdateCacheResult(result.getChunk(), result.getAccessDate());
                                }
                            }
                        }
                    }
                } else {
                    // プロトコル違反。
                    LOG.log(Level.WARNING, "{0} からの返事の型 {1} は期待する型 {2} と異なります。",
                            new Object[] { destination, receivedMail.getMail().get(0).getClass(), GetOrUpdateCacheReply.class });
                    this.network.removeInvalidPeer(destination.getPeer());
                    continue;
                }
            }
        }

        return GetOrUpdateCacheResult.newGiveUp();
    }

}
