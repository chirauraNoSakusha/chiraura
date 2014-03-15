package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.messenger.ReceivedMail;
import nippon.kawauso.chiraura.network.AddressedPeer;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class SimpleRecoveryDriver {

    private static final Logger LOG = Logger.getLogger(SimpleRecoveryDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final SessionManager sessionManager;
    private final TypeRegistry<Chunk.Id<?>> idRegistry;

    SimpleRecoveryDriver(final NetworkWrapper network, final StorageWrapper storage, final SessionManager sessionManager,
            final TypeRegistry<Chunk.Id<?>> idRegistry) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        } else if (sessionManager == null) {
            throw new IllegalArgumentException("Null session manager.");
        } else if (idRegistry == null) {
            throw new IllegalArgumentException("Null id registry.");
        }
        this.network = network;
        this.storage = storage;
        this.sessionManager = sessionManager;
        this.idRegistry = idRegistry;
    }

    boolean isObvious(final SimpleRecoveryOperation operation) throws IOException, InterruptedException {
        return !this.network.dominates(operation.getId().getAddress()) || this.storage.contains(operation.getId());
    }

    SimpleRecoveryResult execute(final SimpleRecoveryOperation operation, final long timeout) throws IOException, InterruptedException {
        final long start = System.currentTimeMillis();

        if (!this.network.dominates(operation.getId().getAddress())) {
            LOG.log(Level.FINEST, "{0} の担当は自分じゃありませんでした。", operation.getId());
            // 同期が必要無いから成功。
            return new SimpleRecoveryResult();
        } else if (this.storage.contains(operation.getId())) {
            LOG.log(Level.FINEST, "既に {0} を持っています。", operation.getId());
            return new SimpleRecoveryResult();
        }

        final InetSocketAddress destination = selectDestination();
        if (destination == null) {
            // 近接個体がいない場合は無条件失敗。
            return SimpleRecoveryResult.newFailure();
        }

        // やりとりの準備。
        final Session session = this.sessionManager.newSession(destination);

        // 手紙の準備。
        final List<Message> mail = new ArrayList<>(2);
        mail.add(new SimpleRecoveryMessage(this.idRegistry, operation.getId()));
        mail.add(new SessionMessage(session));

        // 送受信。
        this.network.sendMail(destination, ConnectionTypes.CONTROL, mail);
        final ReceivedMail receivedMail = this.sessionManager.waitReply(session, start + timeout - System.currentTimeMillis());

        if (receivedMail == null) {
            if (start + timeout <= System.currentTimeMillis()) {
                LOG.log(Level.FINEST, "{0} は時間切れになりました。", operation);
                // 非リレーの時間切れは懲罰対象。
                this.network.removeLostPeer(destination);
            } else {
                // エラー報告が別に上がっているはずなので、ここで個体の削除はしない。
                LOG.log(Level.FINER, "なぜか {0} は失敗しました。", operation);
            }
            return SimpleRecoveryResult.newGiveUp();
        } else {
            final List<Message> reply = receivedMail.getMail();
            if (reply.get(0) instanceof SimpleRecoveryReply) {
                // 通信網の更新。
                LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { destination, operation });
                this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                final SimpleRecoveryReply message = (SimpleRecoveryReply) reply.get(0);
                if (message.isRejected()) {
                    LOG.log(Level.FINEST, "{0} は断られました。", operation);
                    return SimpleRecoveryResult.newGiveUp();
                } else if (message.isNotFound()) {
                    LOG.log(Level.FINEST, "{0} は対象無しと言われました。", operation);
                    return SimpleRecoveryResult.newNotFound();
                } else {
                    if (this.storage.weakWrite(message.getChunk())) {
                        LOG.log(Level.FINEST, "{0} が成功しました。", operation);
                    } else {
                        LOG.log(Level.FINEST, "{0} は必要ありませんでした。", operation);
                    }
                    return new SimpleRecoveryResult();
                }
            } else {
                // プロトコル違反。
                LOG.log(Level.WARNING, "{0} からの返事の型 {1} は期待する型 {2} と異なります。", new Object[] { destination, reply.get(0).getClass(),
                        SimpleRecoveryReply.class });
                this.network.removeInvalidPeer(destination);
                return SimpleRecoveryResult.newGiveUp();
            }
        }
    }

    private InetSocketAddress selectDestination() {
        final List<AddressedPeer> peers = this.network.getBackupNeighbors(Integer.MAX_VALUE);
        if (peers.isEmpty()) {
            return null;
        } else {
            return peers.get(ThreadLocalRandom.current().nextInt(peers.size())).getPeer();
        }
    }

}
