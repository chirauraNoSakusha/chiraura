/**
 * 
 */
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
final class PatchOrAddAndGetCacheDriver {

    private static final Logger LOG = Logger.getLogger(PatchOrAddAndGetCacheDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final BlockingQueue<Operation> operationSink;
    private final SessionManager sessionManager;
    private final TypeRegistry<Chunk> chunkRegistry;

    PatchOrAddAndGetCacheDriver(final NetworkWrapper network, final StorageWrapper storage, final BlockingQueue<Operation> operationSink,
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

    PatchOrAddAndGetCacheResult execute(final PatchOrAddAndGetCacheOperation operation, final long timeout) throws InterruptedException, IOException {
        final long start = System.currentTimeMillis();
        final Address address = operation.getChunk().getId().getAddress();
        final Set<AddressedPeer> usedDestinations = new HashSet<>();

        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < start + timeout) {

            final AddressedPeer destination = this.network.getRoutingDestination(address);

            if (destination == null) {
                LOG.log(Level.FINEST, "{0} の担当は自分でした。", operation.getChunk().getId());
                final StorageWrapper.Result<? extends Mountain> result = this.storage.patchOrWriteAndRead(operation.getChunk());
                if (result.isSuccess()) {
                    LOG.log(Level.FINEST, "{0} を書き換えました。", operation.getChunk().getId());
                    ConcurrentFunctions.completePut(new BackupOneOperation(operation.getChunk().getId()), this.operationSink);
                } else {
                    LOG.log(Level.FINEST, "{0} を書き換えませんでした。", operation.getChunk().getId());
                }
                return new PatchOrAddAndGetCacheResult(result.getChunk(), System.currentTimeMillis());
            }

            if (usedDestinations.contains(destination)) {
                LOG.log(Level.FINEST, "{0} の依頼先 {1} が重複したので諦めます。", new Object[] { operation, destination });
                return PatchOrAddAndGetCacheResult.newGiveUp();
            }

            // やりとりの準備。
            final Session session = this.sessionManager.newSession(destination.getPeer());

            // 手紙の準備。
            final List<Message> mail = new ArrayList<>(2);
            mail.add(new PatchOrAddAndGetCacheMessage(this.chunkRegistry, operation.getChunk()));
            mail.add(new SessionMessage(session));

            LOG.log(Level.FINEST, "{0} を {1} に依頼します。", new Object[] { operation, destination });

            // 送受信。
            this.network.sendMail(destination.getPeer(), ConnectionTypes.DATA, mail);
            final ReceivedMail receivedMail = this.sessionManager.waitReply(session, start + timeout - System.currentTimeMillis());

            if (receivedMail == null) {
                if (start + timeout <= System.currentTimeMillis()) {
                    LOG.log(Level.FINEST, "{0} は時間切れなので諦めます。", operation);
                    return PatchOrAddAndGetCacheResult.newGiveUp();
                } else {
                    LOG.log(Level.FINEST, "{0} を依頼した {1} との間に何か異常がありました。", new Object[] { operation, destination });
                    usedDestinations.add(destination);
                    continue;
                }
            } else {
                if (receivedMail.getMail().get(0) instanceof PatchOrAddAndGetCacheReply) {
                    // 正常。
                    LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { destination, operation });
                    this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                    final PatchOrAddAndGetCacheReply reply = (PatchOrAddAndGetCacheReply) receivedMail.getMail().get(0);
                    if (reply.isRejected()) {
                        LOG.log(Level.FINEST, "{0} は断られました。", operation);
                        return PatchOrAddAndGetCacheResult.newGiveUp();
                    } else if (reply.isGivenUp()) {
                        LOG.log(Level.FINEST, "{0} は諦められました。", operation);
                        return PatchOrAddAndGetCacheResult.newGiveUp();
                    } else {
                        final StorageWrapper.CacheResult<?> result = this.storage.forceWriteCache(reply.getChunk(), reply.getAccessDate());
                        if (result.isSuccess()) {
                            LOG.log(Level.FINEST, "{0} が成功し更新されました。", operation);
                        } else {
                            LOG.log(Level.FINEST, "{0} が成功したけど更新はされませんでした。", operation);
                        }
                        return new PatchOrAddAndGetCacheResult(reply.getChunk(), reply.getAccessDate());
                    }
                } else {
                    // プロトコル違反。
                    LOG.log(Level.WARNING, "{0} からの返事の型 {1} は期待する型 {2} と異なります。", new Object[] { destination, receivedMail.getMail().get(0).getClass(),
                            PatchOrAddAndGetCacheReply.class });
                    this.network.removeInvalidPeer(destination.getPeer());
                    continue;
                }
            }
        }

        return PatchOrAddAndGetCacheResult.newGiveUp();
    }

}
