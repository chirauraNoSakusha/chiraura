package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.messenger.ReceivedMail;
import nippon.kawauso.chiraura.storage.Chunk;
import nippon.kawauso.chiraura.storage.Storage;

/**
 * @author chirauraNoSakusha
 */
final class RecoveryDriver {

    private static final Logger LOG = Logger.getLogger(RecoveryDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final SessionManager sessionManager;
    private final TypeRegistry<Chunk.Id<?>> idRegistry;
    private final BlockingQueue<OutlawReport> outlawReportSink;

    RecoveryDriver(final NetworkWrapper network, final StorageWrapper storage, final SessionManager sessionManager,
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

    <T extends Mountain> RecoveryResult execute(final RecoveryOperation operation, final long timeout) throws InterruptedException, IOException {
        final long start = System.currentTimeMillis();

        if (!this.network.dominates(operation.getDestinationStock().getId().getAddress())) {
            LOG.log(Level.FINEST, "{0} の担当は自分じゃありませんでした。", operation.getDestinationStock().getId());
            // 同期が必要無いから成功。
            return new RecoveryResult();
        }

        final Storage.Index index = this.storage.getIndex(operation.getDestinationStock().getId());
        if (index != null) {
            if (index.getDate() > operation.getDestinationStock().getDate()) {
                LOG.log(Level.FINEST, "自分の方が新しい {0} でした。", operation.getDestinationStock().getId());
                return new RecoveryResult();
            } else if (index.getHashValue().equals(operation.getDestinationStock().getHashValue())) {
                LOG.log(Level.FINEST, "自分の {0} と同じでした。", operation.getDestinationStock().getId());
                return new RecoveryResult();
            }
        }

        // やりとりの準備。
        final Session session = this.sessionManager.newSession(operation.getDestination());

        // 手紙の準備。
        final List<Message> mail = new ArrayList<>(2);
        if (index == null) {
            mail.add(new RecoveryMessage(this.idRegistry, operation.getDestinationStock().getId()));
            LOG.log(Level.FINEST, "{0} から {1} を取り寄せます。", new Object[] { operation.getDestination(), operation.getDestinationStock().getId() });
        } else if (index.getDate() < operation.getDestinationStock().getDate()) {
            if (Mountain.class.isAssignableFrom(index.getId().getChunkClass())) {
                mail.add(new RecoveryMessage(this.idRegistry, index.getId(), index.getDate()));
                LOG.log(Level.FINEST, "{0} から新しい {1} の差分を取り寄せます。", new Object[] { operation.getDestination(), index.getId() });
            } else {
                mail.add(new RecoveryMessage(this.idRegistry, index.getId()));
                LOG.log(Level.FINEST, "{0} から新しい {1} を取り寄せます。", new Object[] { operation.getDestination(), index.getId() });
            }
        } else if (index.getDate() == operation.getDestinationStock().getDate() && !index.getHashValue().equals(operation.getDestinationStock().getHashValue())) {
            mail.add(new RecoveryMessage(this.idRegistry, index.getId()));
            LOG.log(Level.FINEST, "{0} から異なる {1} を取り寄せます。", new Object[] { operation.getDestination(), index.getId() });
        }
        mail.add(new SessionMessage(session));

        // 送受信。
        this.network.sendMail(operation.getDestination(), ConnectionTypes.CONTROL, mail);
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
            return RecoveryResult.newGiveUp();
        } else {
            final List<Message> reply = receivedMail.getMail();
            if (reply.get(0) instanceof RecoveryReply) {
                // 通信網の更新。
                LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { operation.getDestination(), operation });
                this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                @SuppressWarnings("unchecked")
                final RecoveryReply<T> message = (RecoveryReply<T>) reply.get(0);
                if (message.isRejected()) {
                    LOG.log(Level.FINEST, "{0} は断られました。", operation);
                    return RecoveryResult.newGiveUp();
                } else if (message.isNotFound()) {
                    LOG.log(Level.FINEST, "{0} は対象無しと言われました。", operation);
                    return RecoveryResult.newNotFound();
                } else if (message.isGet()) {
                    final StorageWrapper.Result<?> result = this.storage.update(message.getChunk());
                    if (result.getChunk().equals(message.getChunk())) {
                        LOG.log(Level.FINEST, "{0} が成功しました。", operation);
                        return new RecoveryResult();
                    } else {
                        LOG.log(Level.FINEST, "{0} は不完全に終わりました。", operation);
                        return RecoveryResult.newFailure();
                    }
                } else {
                    final StorageWrapper.Result<T> result = this.storage.patch(message.getId(), message.getDiffs());
                    if (result.isNotFound()) {
                        LOG.log(Level.FINEST, "{0} の差分を適用する対象がありませんでした。", operation);
                        return RecoveryResult.newFailure();
                    } else if (result.getChunk().getHashValue().equals(message.getHashValue())) {
                        LOG.log(Level.FINEST, "{0} が差分で成功しました。", operation);
                        return new RecoveryResult();
                    } else {
                        LOG.log(Level.FINEST, "{0} は差分で不完全に終わりました。", operation);
                        return RecoveryResult.newFailure();
                    }
                }
            } else {
                // プロトコル違反。
                LOG.log(Level.WARNING, "{0} からの返事の型 {1} は期待する型 {2} と異なります。", new Object[] { operation.getDestination(), reply.get(0).getClass(),
                        RecoveryReply.class });
                ConcurrentFunctions.completePut(new OutlawReport(operation.getDestination()), this.outlawReportSink);
                return RecoveryResult.newGiveUp();
            }
        }
    }

}
