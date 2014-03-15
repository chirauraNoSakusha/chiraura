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
final class AddCacheDriver {

    private static final Logger LOG = Logger.getLogger(AddCacheDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final BlockingQueue<Operation> operationSink;
    private final SessionManager sessionManager;
    private final TypeRegistry<Chunk> chunkRegistry;

    AddCacheDriver(final NetworkWrapper network, final StorageWrapper storage, final BlockingQueue<Operation> operationSink,
            final SessionManager sessionManager, final TypeRegistry<Chunk> chunkRegistry) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        } else if (operationSink == null) {
            throw new IllegalArgumentException("Null operation sink.");
        } else if (sessionManager == null) {
            throw new IllegalArgumentException("Null session manager.");
        } else if (chunkRegistry == null) {
            throw new IllegalArgumentException("Null chunk registry.");
        }
        this.network = network;
        this.storage = storage;
        this.operationSink = operationSink;
        this.sessionManager = sessionManager;
        this.chunkRegistry = chunkRegistry;
    }

    boolean isObvious(final AddCacheOperation operation) throws IOException, InterruptedException {
        return this.storage.contains(operation.getChunk().getId());
    }

    AddCacheResult execute(final AddCacheOperation operation, final long timeout) throws InterruptedException, IOException {
        final long start = System.currentTimeMillis();
        final Address address = operation.getChunk().getId().getAddress();
        final Set<AddressedPeer> usedDestinations = new HashSet<>();

        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < start + timeout) {

            final AddressedPeer destination = this.network.getRoutingDestination(address);

            if (destination == null) {
                LOG.log(Level.FINEST, "{0} の担当は自分でした。", operation);
                if (this.storage.weakWrite(operation.getChunk())) {
                    LOG.log(Level.FINEST, "{0} を追加できました。", operation.getChunk().getId());
                    ConcurrentFunctions.completePut(new BackupOneOperation(operation.getChunk().getId()), this.operationSink);
                    return new AddCacheResult(System.currentTimeMillis());
                } else {
                    LOG.log(Level.FINEST, "{0} を追加できませんでした。", operation.getChunk().getId());
                    return AddCacheResult.newFailure();
                }
            }

            final StorageWrapper.CacheResult<?> cache = this.storage.readCache(operation.getChunk().getId());
            if (cache.hasInfo()) {
                LOG.log(Level.FINEST, "ついさっき {0} の情報を更新してました。", operation.getChunk().getId());
                if (!cache.isNotFound()) {
                    LOG.log(Level.FINEST, "{0} の複製がありました。", operation.getChunk().getId());
                    return AddCacheResult.newFailure();
                }
            }

            if (usedDestinations.contains(destination)) {
                LOG.log(Level.FINEST, "{0} の依頼先 {1} が重複したので諦めます。", new Object[] { operation, destination });
                return AddCacheResult.newGiveUp();
            }

            // やりとりの準備。
            final Session session = this.sessionManager.newSession(destination.getPeer());

            // 手紙の準備。
            final List<Message> mail = new ArrayList<>(2);
            mail.add(new AddCacheMessage(this.chunkRegistry, operation.getChunk()));
            mail.add(new SessionMessage(session));

            LOG.log(Level.FINEST, "{0} を {1} に依頼します。", new Object[] { operation, destination });

            // 送受信。
            this.network.sendMail(destination.getPeer(), ConnectionTypes.DATA, mail);
            final ReceivedMail receivedMail = this.sessionManager.waitReply(session, start + timeout - System.currentTimeMillis());

            if (receivedMail == null) {
                if (start + timeout <= System.currentTimeMillis()) {
                    LOG.log(Level.FINEST, "{0} は時間切れなので諦めます。", operation);
                    return AddCacheResult.newGiveUp();
                } else {
                    LOG.log(Level.FINEST, "{0} を依頼した {1} との間に何か異常がありました。", new Object[] { operation, destination });
                    usedDestinations.add(destination);
                    continue;
                }
            } else {
                if (receivedMail.getMail().get(0) instanceof AddCacheReply) {
                    // 正常。
                    LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { destination, operation });
                    this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                    final AddCacheReply reply = (AddCacheReply) receivedMail.getMail().get(0);
                    if (reply.isRejected()) {
                        LOG.log(Level.FINEST, "{0} は断られました。", operation);
                        return AddCacheResult.newGiveUp();
                    } else if (reply.isGivenUp()) {
                        LOG.log(Level.FINEST, "{0} は諦められました。", operation);
                        return AddCacheResult.newGiveUp();
                    } else if (reply.isSuccess()) {
                        LOG.log(Level.FINEST, "{0} が成功しました。", operation);
                        // 複製。
                        this.storage.forceWriteCache(operation.getChunk(), reply.getAccessDate());
                        return new AddCacheResult(reply.getAccessDate());
                    } else {
                        LOG.log(Level.FINEST, "{0} は失敗しました。", operation);
                        return AddCacheResult.newFailure();
                    }
                } else {
                    // プロトコル違反。
                    LOG.log(Level.WARNING, "{0} からの返事の型 {1} は期待する型 {2} と異なります。", new Object[] { destination, receivedMail.getMail().get(0).getClass(),
                            AddCacheReply.class });
                    this.network.removeInvalidPeer(destination.getPeer());
                    continue;
                }
            }
        }

        return AddCacheResult.newGiveUp();
    }

}
