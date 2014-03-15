package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.messenger.ReceivedMail;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class BackupDriver {

    private static final Logger LOG = Logger.getLogger(BackupDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final SessionManager sessionManager;
    private final TypeRegistry<Chunk> chunkRegistry;

    BackupDriver(final NetworkWrapper network, final StorageWrapper storage, final SessionManager sessionManager, final TypeRegistry<Chunk> chunkRegistry) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        } else if (sessionManager == null) {
            throw new IllegalArgumentException("Null session manager.");
        } else if (chunkRegistry == null) {
            throw new IllegalArgumentException("Null chunk registry.");
        }
        this.network = network;
        this.storage = storage;
        this.sessionManager = sessionManager;
        this.chunkRegistry = chunkRegistry;
    }

    BackupResult execute(final BackupOperation operation, final long timeout) throws InterruptedException, IOException {
        final long start = System.currentTimeMillis();

        if (!this.network.dominates(operation.getDemand().getId().getAddress())) {
            LOG.log(Level.FINEST, "{0} の担当は自分じゃありませんでした。", operation.getDemand().getId());
            // 同期が必要無いから成功。
            return new BackupResult();
        }

        final BackupMessage<?> message;
        final Chunk chunk = this.storage.read(operation.getDemand().getId());
        if (chunk == null) {
            LOG.log(Level.FINEST, "{0} を持ってませんでした。", operation.getDemand().getId());
            return BackupResult.newFailure();
        } else if (operation.getDemand().isStocked()) {
            if (chunk.getDate() < operation.getDemand().getStockDate()) {
                LOG.log(Level.FINEST, "自分の方が古い {0} でした。", operation.getDemand().getId());
                return new BackupResult();
            } else if (chunk.getDate() > operation.getDemand().getStockDate()) {
                if (chunk instanceof Mountain) {
                    LOG.log(Level.FINEST, "{0} に {1} の差分を送ります。", new Object[] { operation.getDestination(), operation.getDemand().getId() });
                    message = new BackupMessage<>(this.chunkRegistry, (Mountain) chunk, operation.getDemand().getStockDate());
                } else {
                    LOG.log(Level.FINEST, "{0} に新しい {1} を送ります。", new Object[] { operation.getDestination(), operation.getDemand().getId() });
                    message = new BackupMessage<>(this.chunkRegistry, chunk);
                }
            } else if (chunk.getHashValue().equals(operation.getDemand().getStockHashValue())) {
                LOG.log(Level.FINEST, "自分の {0} と同じでした。", operation.getDemand().getId());
                return new BackupResult();
            } else {
                LOG.log(Level.FINEST, "{0} に異なる {1} を送ります。", new Object[] { operation.getDestination(), operation.getDemand().getId() });
                message = new BackupMessage<>(this.chunkRegistry, chunk);
            }
        } else {
            LOG.log(Level.FINEST, "{0} に {1} を送ります。", new Object[] { operation.getDestination(), operation.getDemand().getId() });
            message = new BackupMessage<>(this.chunkRegistry, chunk);
        }

        // やりとりの準備。
        final Session session = this.sessionManager.newSession(operation.getDestination());

        // 手紙の準備。
        final List<Message> mail = new ArrayList<>(2);
        mail.add(message);
        mail.add(new SessionMessage(session));

        // 送受信。
        this.network.sendMail(operation.getDestination(), ConnectionTypes.DATA, mail);
        final ReceivedMail receivedMail = this.sessionManager.waitReply(session, start + timeout - System.currentTimeMillis());

        if (receivedMail == null) {
            if (start + timeout <= System.currentTimeMillis()) {
                LOG.log(Level.FINEST, "{0} は時間切れになりました。", operation);
                // 非リレーの時間切れは懲罰対象。
                this.network.removeLostPeer(operation.getDestination());
            } else {
                // エラー報告が別に上がっているはずなので、ここで個体の削除はしない。
                LOG.log(Level.FINER, "なぜか {0} は失敗しました。", operation);
            }
            return BackupResult.newGiveUp();
        } else {
            if (receivedMail.getMail().get(0) instanceof BackupReply) {
                LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { operation.getDestination(), operation });
                this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                final BackupReply reply = (BackupReply) receivedMail.getMail().get(0);
                if (reply.isRejected()) {
                    LOG.log(Level.FINEST, "{0} は断られました。", operation);
                    return BackupResult.newGiveUp();
                } else if (reply.isGivenUp()) {
                    LOG.log(Level.FINEST, "{0} は諦められました。", operation);
                    return BackupResult.newGiveUp();
                } else if (reply.isSuccess()) {
                    LOG.log(Level.FINEST, "{0} が成功しました。", operation);
                    return new BackupResult();
                } else {
                    final StorageWrapper.Result<?> result = this.storage.update(reply.getChunk());
                    if (result.getChunk().equals(reply.getChunk())) {
                        LOG.log(Level.FINEST, "{0} は一悶着の末に成功しました。", operation);
                        return new BackupResult();
                    } else {
                        LOG.log(Level.FINEST, "{0} は失敗しました。", operation);
                        return BackupResult.newFailure();
                    }
                }
            } else {
                // プロトコル違反。
                LOG.log(Level.WARNING, "{0} からの返事の型 {1} は期待する型 {2} と異なります。", new Object[] { operation.getDestination(),
                        receivedMail.getMail().get(0).getClass(), BackupReply.class });
                this.network.removeInvalidPeer(operation.getDestination());
                return BackupResult.newGiveUp();
            }
        }
    }

}
