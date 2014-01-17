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
 * @author chirauraNoSakusha
 */
final class FirstAccessDriver {

    private static final Logger LOG = Logger.getLogger(FirstAccessDriver.class.getName());

    // 参照。
    private final SessionManager sessionManager;
    private final NetworkWrapper network;

    FirstAccessDriver(final SessionManager sessionManager, final NetworkWrapper network) {
        if (sessionManager == null) {
            throw new IllegalArgumentException("Null session manager.");
        } else if (network == null) {
            throw new IllegalArgumentException("Null network.");
        }

        this.sessionManager = sessionManager;
        this.network = network;
    }

    FirstAccessResult execute(final FirstAccessOperation operation, final long timeout) throws InterruptedException {
        final long start = System.currentTimeMillis();

        // やりとりの準備。
        final Session session = this.sessionManager.newSession(operation.getDestination());

        // 手紙の準備。
        final List<Message> mail = new ArrayList<>(2);
        mail.add(new AddressAccessMessage(this.network.getSelfAddress().subtractOne()));
        mail.add(new SessionMessage(session));

        LOG.log(Level.FINEST, "{0} を {1} に依頼します。", new Object[] { operation, operation.getDestination() });

        // 送受信。
        this.network.sendMail(operation.getDestination(), ConnectionTypes.CONTROL, mail);
        final ReceivedMail receivedMail = this.sessionManager.waitReply(session, start + timeout - System.currentTimeMillis());

        if (receivedMail == null) {
            if (start + timeout <= System.currentTimeMillis()) {
                LOG.log(Level.FINEST, "{0} は時間切れになりました。", operation);
            } else {
                LOG.log(Level.FINER, "なぜか {0} は失敗しました。", operation);
            }
            return FirstAccessResult.newGiveUp();
        } else {
            if (receivedMail.getMail().get(0) instanceof AddressAccessReply) {
                // 正常。
                LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { operation.getDestination(), operation });
                this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                final AddressAccessReply reply = (AddressAccessReply) receivedMail.getMail().get(0);
                if (reply.isRejected()) {
                    LOG.log(Level.FINEST, "{0} は断られました。", operation);
                    return FirstAccessResult.newGiveUp();
                } else if (reply.isGivenUp()) {
                    LOG.log(Level.FINEST, "{0} は諦められました。", operation);
                    return FirstAccessResult.newGiveUp();
                } else {
                    LOG.log(Level.FINEST, "{0} が成功しました。", operation);
                    if (reply.getManager() != null) {
                        addPeer(reply.getManager());
                    }
                    for (final AddressedPeer peer : reply.getPeers()) {
                        addPeer(peer);
                    }
                    if (reply.getManager() == null) {
                        return new FirstAccessResult(reply.getPeers());
                    } else {
                        addPeer(reply.getManager());
                        return new FirstAccessResult(reply.getPeers());
                    }
                }
            } else {
                // プロトコル違反なので、通信先を取り除いて再試行。
                LOG.log(Level.WARNING, "{0} からの返信 ( {1} ) は期待する内容 ( {2} ) と異なります。", new Object[] { operation.getDestination(),
                        receivedMail.getMail().get(0).getClass(), AddressAccessReply.class });
                this.network.removeInvalidPeer(operation.getDestination());
                return FirstAccessResult.newGiveUp();
            }
        }
    }

    private void addPeer(final AddressedPeer peer) {
        if (this.network.addPeer(peer)) {
            LOG.log(Level.FINER, "{0} を通信網に加えました。", peer);
        }
    }

}
