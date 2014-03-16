package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Mountain;
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
final class PatchAndGetOrUpdateCacheDriver {

    private static final Logger LOG = Logger.getLogger(PatchAndGetOrUpdateCacheDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final BlockingQueue<Operation> operationSink;
    private final SessionManager sessionManager;
    private final TypeRegistry<Chunk.Id<?>> idRegistry;
    private final GetCacheBlockingDriver getDriver;
    private final PatchOrAddAndGetCacheBlockingDriver patchAndGetDriver;
    private final BlockingQueue<OutlawReport> outlawReportSink;

    PatchAndGetOrUpdateCacheDriver(final NetworkWrapper network, final StorageWrapper storage, final BlockingQueue<Operation> operationSink,
            final SessionManager sessionManager, final TypeRegistry<Chunk.Id<?>> idRegistry, final GetCacheBlockingDriver getDriver,
            final PatchOrAddAndGetCacheBlockingDriver patchAndGetDriver, final BlockingQueue<OutlawReport> outlawReportSink) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        } else if (operationSink == null) {
            throw new IllegalArgumentException("Null operation sink.");
        } else if (sessionManager == null) {
            throw new IllegalArgumentException("Null session manager.");
        } else if (idRegistry == null) {
            throw new IllegalArgumentException("Null id registry.");
        } else if (getDriver == null) {
            throw new IllegalArgumentException("Null get driver.");
        } else if (patchAndGetDriver == null) {
            throw new IllegalArgumentException("Null patch and get driver.");
        } else if (outlawReportSink == null) {
            throw new IllegalArgumentException("Null outlaw report sink.");
        }
        this.network = network;
        this.storage = storage;
        this.operationSink = operationSink;
        this.sessionManager = sessionManager;
        this.idRegistry = idRegistry;
        this.getDriver = getDriver;
        this.patchAndGetDriver = patchAndGetDriver;
        this.outlawReportSink = outlawReportSink;
    }

    <T extends Mountain> PatchAndGetOrUpdateCacheResult execute(final PatchAndGetOrUpdateCacheOperation<T> operation, final long timeout)
            throws InterruptedException, IOException {
        final long start = System.currentTimeMillis();
        final Address address = operation.getId().getAddress();
        final Set<AddressedPeer> usedDestinations = new HashSet<>();

        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < start + timeout) {

            final AddressedPeer destination = this.network.getRoutingDestination(address);

            if (destination == null) {
                LOG.log(Level.FINEST, "{0} の担当は自分でした。", operation.getId());
                final StorageWrapper.Result<T> result = this.storage.patch(operation.getId(), operation.getDiff());
                if (result.isNotFound()) {
                    LOG.log(Level.FINEST, "{0} はありませんでした。", operation.getId());
                    return PatchAndGetOrUpdateCacheResult.newNotFound(System.currentTimeMillis());
                } else if (result.isSuccess()) {
                    LOG.log(Level.FINEST, "{0} に差分を適用できました。", operation.getId());
                    ConcurrentFunctions.completePut(new BackupOneOperation(operation.getId()), this.operationSink);
                    return new PatchAndGetOrUpdateCacheResult(true, result.getChunk(), System.currentTimeMillis());
                } else {
                    LOG.log(Level.FINEST, "{0} に差分を適用できませんでした。", operation.getId());
                    return new PatchAndGetOrUpdateCacheResult(false, result.getChunk(), System.currentTimeMillis());
                }
            }

            final StorageWrapper.CacheResult<? extends Mountain> cache = this.storage.readCache(operation.getId());
            if (cache.hasInfo()) {
                LOG.log(Level.FINEST, "ついさっき {0} の情報を更新してました。", operation.getId());
                if (cache.isNotFound()) {
                    LOG.log(Level.FINEST, "{0} は無いらしいです。", operation.getId());
                    return PatchAndGetOrUpdateCacheResult.newNotFound(cache.getAccessDate());
                } else if (!cache.getChunk().patchable(operation.getDiff())) {
                    LOG.log(Level.FINEST, "{0} の複製に差分を適用できません。", operation.getId());
                    return new PatchAndGetOrUpdateCacheResult(false, cache.getChunk(), cache.getAccessDate());
                }
            }

            if (usedDestinations.contains(destination)) {
                LOG.log(Level.FINEST, "{0} の依頼先 {1} が重複したので諦めます。", new Object[] { operation, destination });
                return PatchAndGetOrUpdateCacheResult.newGiveUp();
            }

            // やりとりの準備。
            final Session session = this.sessionManager.newSession(destination.getPeer());

            // 手紙の準備。
            final List<Message> mail = new ArrayList<>(2);
            final Mountain chunk = this.storage.read(operation.getId());
            if (chunk == null) {
                mail.add(new PatchAndGetOrUpdateCacheMessage<>(this.idRegistry, operation.getId(), operation.getDiff()));
            } else {
                mail.add(new PatchAndGetOrUpdateCacheMessage<>(this.idRegistry, operation.getId(), operation.getDiff(), chunk.getDate()));
            }
            mail.add(new SessionMessage(session));

            LOG.log(Level.FINEST, "{0} を {1} に依頼します。", new Object[] { operation, destination });

            // 送受信。
            this.network.sendMail(destination.getPeer(), ConnectionTypes.DATA, mail);
            final ReceivedMail receivedMail = this.sessionManager.waitReply(session, start + timeout - System.currentTimeMillis());

            if (receivedMail == null) {
                if (start + timeout <= System.currentTimeMillis()) {
                    LOG.log(Level.FINEST, "{0} は時間切れなので諦めます。", operation);
                    return PatchAndGetOrUpdateCacheResult.newGiveUp();
                } else {
                    LOG.log(Level.FINEST, "{0} を依頼した {1} との間に何か異常がありました。", new Object[] { operation, destination });
                    usedDestinations.add(destination);
                    continue;
                }
            } else {
                if (receivedMail.getMail().get(0) instanceof PatchAndGetOrUpdateCacheReply) {
                    // 正常。
                    LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { destination, operation });
                    this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                    @SuppressWarnings("unchecked")
                    final PatchAndGetOrUpdateCacheReply<T> reply = (PatchAndGetOrUpdateCacheReply<T>) receivedMail.getMail().get(0);
                    if (reply.isRejected()) {
                        LOG.log(Level.FINEST, "{0} は断られました。", operation);
                        return PatchAndGetOrUpdateCacheResult.newGiveUp();
                    } else if (reply.isGivenUp()) {
                        LOG.log(Level.FINEST, "{0} は諦められました。", operation);
                        return PatchAndGetOrUpdateCacheResult.newGiveUp();
                    } else if (reply.isNotFound()) {
                        final StorageWrapper.CacheResult<T> cache0 = this.storage.addNotFoundCache(operation.getId(), reply.getAccessDate());
                        if (cache0.isNotFound()) {
                            LOG.log(Level.FINEST, "{0} は対象無しと言われました。", operation);
                            return PatchAndGetOrUpdateCacheResult.newNotFound(reply.getAccessDate());
                        } else {
                            LOG.log(Level.FINEST, "{0} は対象無しと言われたけど複製がありました。", operation);
                            return new PatchAndGetOrUpdateCacheResult(false, cache0.getChunk(), cache0.getAccessDate());
                        }
                    } else if (reply.isGet()) {
                        final StorageWrapper.CacheResult<T> cache0 = this.storage.forceWriteCache(reply.getChunk(), reply.getAccessDate());
                        if (reply.isSuccess()) {
                            if (cache0.isSuccess()) {
                                LOG.log(Level.FINEST, "{0} の差分適用が成功し更新されました。", operation);
                            } else {
                                LOG.log(Level.FINEST, "{0} の差分適用が成功したけど更新はされませんでした。", operation);
                            }
                        } else {
                            if (cache0.isSuccess()) {
                                LOG.log(Level.FINEST, "{0} の差分適用が失敗したけど更新されました。", operation);
                            } else {
                                LOG.log(Level.FINEST, "{0} の差分適用が失敗し更新はされませんでした。", operation);
                            }
                        }
                        return new PatchAndGetOrUpdateCacheResult(reply.isSuccess(), cache0.getChunk(), cache0.getAccessDate());
                    } else {
                        if (reply.isSuccess()) {
                            LOG.log(Level.FINEST, "{0} の差分適用が成功しました。", operation);
                        } else {
                            LOG.log(Level.FINEST, "{0} の差分適用が失敗しました。", operation);
                        }
                        final Mountain before = this.storage.read(operation.getId());
                        if (before == null) {
                            LOG.log(Level.FINEST, "{0} の応答として差分が来たけど基礎を持ってなかったので一から取り寄せます。", operation);
                            final GetCacheOperation subOperation = new GetCacheOperation(operation.getId());
                            final GetCacheResult subResult = this.getDriver.execute(subOperation, start + timeout - System.currentTimeMillis());
                            if (subResult.isGivenUp()) {
                                LOG.log(Level.FINEST, "{0} の更新は一悶着の末に諦められました。", operation);
                                return PatchAndGetOrUpdateCacheResult.newGiveUp();
                            } else if (subResult.isNotFound()) {
                                LOG.log(Level.FINEST, "{0} の更新は一悶着の末に対象無しと言われました。", operation);
                                return PatchAndGetOrUpdateCacheResult.newNotFound(subResult.getAccessDate());
                            } else {
                                LOG.log(Level.FINEST, "{0} の更新が一悶着の末に成功しました。", operation);
                                return new PatchAndGetOrUpdateCacheResult(reply.isSuccess(), (Mountain) subResult.getChunk(), subResult.getAccessDate());
                            }
                        } else {
                            final Mountain after = before.copy();
                            for (final Mountain.Dust<?> diff : reply.getDiffs()) {
                                after.patch(diff);
                            }
                            if (after.getHashValue().equals(reply.getHashValue())) {
                                LOG.log(Level.FINEST, "{0} の更新が成功しました。", operation);
                                this.storage.forceWriteCache(after, reply.getAccessDate());
                                return new PatchAndGetOrUpdateCacheResult(reply.isSuccess(), after, reply.getAccessDate());
                            } else {
                                LOG.log(Level.FINEST, "{0} の応答として来た差分だけでは同期できなかったので、もいっちょ頑張ります。", operation);
                                final PatchOrAddAndGetCacheResult result = this.patchAndGetDriver.execute(new PatchOrAddAndGetCacheOperation(before),
                                        start + timeout - System.currentTimeMillis());
                                if (result.isGivenUp()) {
                                    LOG.log(Level.FINEST, "{0} の更新は一悶着の末に諦められました。", operation);
                                    return PatchAndGetOrUpdateCacheResult.newGiveUp();
                                } else {
                                    LOG.log(Level.FINEST, "{0} の更新が一悶着の末に成功しました。", operation);
                                    return new PatchAndGetOrUpdateCacheResult(reply.isSuccess(), result.getChunk(), result.getAccessDate());
                                }
                            }
                        }
                    }
                } else {
                    // プロトコル違反。
                    LOG.log(Level.WARNING, "{0} からの返事の型 {1} は期待する型 {2} と異なります。", new Object[] { destination, receivedMail.getMail().get(0).getClass(),
                            PatchAndGetOrUpdateCacheReply.class });
                    ConcurrentFunctions.completePut(new OutlawReport(destination.getPeer()), this.outlawReportSink);
                    continue;
                }
            }
        }

        return PatchAndGetOrUpdateCacheResult.newGiveUp();
    }

}
