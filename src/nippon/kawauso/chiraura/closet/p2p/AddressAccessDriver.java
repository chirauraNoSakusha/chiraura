package nippon.kawauso.chiraura.closet.p2p;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.messenger.ReceivedMail;
import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * 論理位置を支配する個体と連絡を取り、その個体が把握している個体一覧を取得する。
 * @author chirauraNoSakusha
 */
final class AddressAccessDriver {

    private static final Logger LOG = Logger.getLogger(AddressAccessDriver.class.getName());

    // 参照。
    private final SessionManager sessionManager;
    private final NetworkWrapper network;

    AddressAccessDriver(final SessionManager sessionManager, final NetworkWrapper network) {
        if (sessionManager == null) {
            throw new IllegalArgumentException("Null session manager.");
        } else if (network == null) {
            throw new IllegalArgumentException("Null network.");
        }

        this.sessionManager = sessionManager;
        this.network = network;
    }

    boolean isObvious(final AddressAccessOperation operation) {
        return this.network.dominates(operation.getAddress());
    }

    AddressAccessResult execute(final AddressAccessOperation operation, final long timeout) throws InterruptedException {
        final long start = System.currentTimeMillis();
        final Set<AddressedPeer> usedDestinations = new HashSet<>();

        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() <= start + timeout) {

            final AddressedPeer destination = this.network.getRoutingDestination(operation.getAddress());

            if (destination == null) {
                LOG.log(Level.FINEST, "{0} は我が領土でした。", operation.getAddress());
                return new AddressAccessResult(null, this.network.getImportantPeers());
            }

            if (usedDestinations.contains(destination)) {
                LOG.log(Level.FINEST, "{0} の依頼先 {1} が重複したので諦めます。", new Object[] { operation, destination });
                return AddressAccessResult.newGiveUp();
            }

            // やりとりの準備。
            final Session session = this.sessionManager.newSession(destination.getPeer());

            // 手紙の準備。
            final List<Message> mail = new ArrayList<>(2);
            mail.add(new AddressAccessMessage(operation.getAddress()));
            mail.add(new SessionMessage(session));

            LOG.log(Level.FINEST, "{0} を {1} に依頼します。", new Object[] { operation, destination });

            // 送受信。
            this.network.sendMail(destination.getPeer(), ConnectionTypes.CONTROL, mail);
            final ReceivedMail receivedMail = this.sessionManager.waitReply(session, start + timeout - System.currentTimeMillis());

            if (receivedMail == null) {
                if (start + timeout <= System.currentTimeMillis()) {
                    LOG.log(Level.FINEST, "{0} は時間切れなので諦めます。", operation);
                    return AddressAccessResult.newGiveUp();
                } else {
                    LOG.log(Level.FINEST, "{0} を依頼した {1} との間に何か異常がありました。", new Object[] { operation, destination });
                    usedDestinations.add(destination);
                    continue;
                }
            } else {
                if (receivedMail.getMail().get(0) instanceof AddressAccessReply) {
                    // 正常。
                    LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { destination, operation });
                    this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                    final AddressAccessReply reply = (AddressAccessReply) receivedMail.getMail().get(0);
                    if (reply.isRejected()) {
                        LOG.log(Level.FINEST, "{0} は断られました。", operation);
                        return AddressAccessResult.newGiveUp();
                    } else if (reply.isGivenUp()) {
                        LOG.log(Level.FINEST, "{0} は諦められました。", operation);
                        return AddressAccessResult.newGiveUp();
                    } else {
                        LOG.log(Level.FINEST, "{0} が成功しました。", operation);
                        if (reply.getManager() != null) {
                            addPeer(reply.getManager());
                        }
                        for (final AddressedPeer peer : reply.getPeers()) {
                            addPeer(peer);
                        }
                        if (reply.getManager() == null) {
                            return new AddressAccessResult(destination, reply.getPeers());
                        } else {
                            addPeer(reply.getManager());
                            return new AddressAccessResult(reply.getManager(), reply.getPeers());
                        }
                    }
                } else {
                    // プロトコル違反なので、通信先を取り除いて再試行。
                    LOG.log(Level.WARNING, "{0} からの返信 ( {1} ) は期待する内容 ( {2} ) と異なります。", new Object[] { destination, receivedMail.getMail().get(0).getClass(),
                            AddressAccessReply.class });
                    this.network.removeInvalidPeer(destination.getPeer());
                    continue;
                }
            }
        }

        return AddressAccessResult.newGiveUp();
    }

    private void addPeer(final AddressedPeer peer) {
        if (this.network.addPeer(peer)) {
            LOG.log(Level.FINER, "{0} を通信網に加えました。", peer);
        }
    }

}
