/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.messenger.ReceivedMail;
import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * 個体へ渡りを付け、個体一覧を取得する。
 * @author chirauraNoSakusha
 */
final class PeerAccessDriver {

    private static final Logger LOG = Logger.getLogger(PeerAccessDriver.class.getName());

    // 参照。
    private final SessionManager sessionManager;
    private final NetworkWrapper network;

    PeerAccessDriver(final SessionManager sessionManager, final NetworkWrapper network) {
        if (sessionManager == null) {
            throw new IllegalArgumentException("Null session manager.");
        } else if (network == null) {
            throw new IllegalArgumentException("Null network.");
        }

        this.sessionManager = sessionManager;
        this.network = network;
    }

    PeerAccessResult execute(final PeerAccessOperation operation, final long timeout) throws InterruptedException {
        final long start = System.currentTimeMillis();

        // やりとりの準備。
        final Session session = this.sessionManager.newSession(operation.getDestination());

        // 手紙の準備。
        final List<Message> mail = new ArrayList<>(2);
        mail.add(PeerAccessMessage.INSTANCE);
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
            return PeerAccessResult.newGiveUp();
        } else {
            final List<Message> reply = receivedMail.getMail();
            if (reply.get(0) instanceof PeerAccessReply) {
                // 通信網の更新。
                LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { operation.getDestination(), operation });
                this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                final PeerAccessReply message = (PeerAccessReply) reply.get(0);
                LOG.log(Level.FINEST, "{0} が成功しました。", operation);
                for (final AddressedPeer peer : message.getPeers()) {
                    if (this.network.addPeer(peer)) {
                        LOG.log(Level.FINER, "{0} を通信網に加えました。", peer);
                    }
                }
                return new PeerAccessResult(message.getPeers());
            } else {
                // プロトコル違反。
                LOG.log(Level.WARNING, "{0} からの返事の型 {1} は期待する型 {2} と異なります。", new Object[] { operation.getDestination(), reply.get(0).getClass(),
                        PeerAccessReply.class });
                this.network.removeInvalidPeer(operation.getDestination());
                return PeerAccessResult.newGiveUp();
            }
        }
    }

}
