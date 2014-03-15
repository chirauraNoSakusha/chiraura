package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.container.Pair;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.messenger.ConnectionTypes;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.messenger.ReceivedMail;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class CheckDemandDriver {

    private static final Logger LOG = Logger.getLogger(CheckDemandDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final SessionManager sessionManager;
    private final TypeRegistry<Chunk.Id<?>> idRegistry;

    private final int entryLimit;

    CheckDemandDriver(final NetworkWrapper network, final StorageWrapper storage, final SessionManager sessionManager,
            final TypeRegistry<Chunk.Id<?>> idRegistry, final int entryLimit) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        } else if (sessionManager == null) {
            throw new IllegalArgumentException("Null session manager.");
        } else if (idRegistry == null) {
            throw new IllegalArgumentException("Null id registry.");
        } else if (entryLimit < 0) {
            throw new IllegalArgumentException("Negative entry limit ( " + entryLimit + " ).");
        }
        this.network = network;
        this.storage = storage;
        this.sessionManager = sessionManager;
        this.idRegistry = idRegistry;

        this.entryLimit = entryLimit;
    }

    CheckDemandResult execute(final CheckDemandOperation operation, final long timeout) throws InterruptedException, IOException {
        final long start = System.currentTimeMillis();

        // やりとりの準備。
        final Session session = this.sessionManager.newSession(operation.getDestination());

        // 手紙の準備。
        final List<Message> mail = new ArrayList<>(2);
        final Pair<Address, Address> domain = this.network.getDomain();
        final List<StockEntry> entries = StockEntry.getStockedEntries(this.storage, domain.getFirst(), domain.getSecond(), this.entryLimit,
                new ArrayList<StockEntry>(0), this.idRegistry);
        if (entries.isEmpty()) {
            // やるだけ無駄なんで止める。
            LOG.log(Level.FINEST, "{0} に必要な [{1}, {2}] の複製候補はありませんでした。", new Object[] { operation, domain.getFirst(), domain.getSecond() });
            return new CheckDemandResult(new ArrayList<DemandEntry>(0));
        }
        mail.add(new CheckDemandMessage(domain.getFirst(), domain.getSecond(), entries));
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
                // 通信異常。
                // エラー報告が別に上がっているはずなので、ここで個体の削除はしない。
                LOG.log(Level.FINER, "なぜか {0} は失敗しました。", operation);
            }
            return CheckDemandResult.newGiveUp();
        } else {
            if (receivedMail.getMail().get(0) instanceof CheckDemandReply) {
                // 通信網の更新。
                LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { operation.getDestination(), operation });
                this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                final CheckDemandReply reply = (CheckDemandReply) receivedMail.getMail().get(0);
                if (reply.isRejected()) {
                    LOG.log(Level.FINEST, "{0} は断られました。", operation);
                    return CheckDemandResult.newGiveUp();
                } else if (reply.isGivenUp()) {
                    LOG.log(Level.FINEST, "{0} は諦められました。", operation);
                    return CheckDemandResult.newGiveUp();
                } else {
                    LOG.log(Level.FINEST, "{0} が成功しました。", operation);
                    return new CheckDemandResult(reply.getDemands());
                }
            } else {
                // プロトコル違反。
                LOG.log(Level.WARNING, "{0} からの返事の型 {1} は期待する型 {2} と異なります。", new Object[] { operation.getDestination(),
                        receivedMail.getMail().get(0).getClass(), CheckDemandReply.class });
                this.network.removeInvalidPeer(operation.getDestination());
                return CheckDemandResult.newGiveUp();
            }
        }
    }

}
