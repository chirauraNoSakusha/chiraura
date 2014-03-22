package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

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
final class CheckOneDemandDriver {

    private static final Logger LOG = Logger.getLogger(CheckOneDemandDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final SessionManager sessionManager;
    private final TypeRegistry<Chunk.Id<?>> idRegistry;
    private final BlockingQueue<OutlawReport> outlawReportSink;

    CheckOneDemandDriver(final NetworkWrapper network, final StorageWrapper storage, final SessionManager sessionManager,
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

    CheckOneDemandResult execute(final CheckOneDemandOperation operation, final long timeout) throws InterruptedException, IOException {
        final long start = System.currentTimeMillis();

        // やりとりの準備。
        final Session session = this.sessionManager.newSession(operation.getDestination());

        // 手紙の準備。
        final List<Message> mail = new ArrayList<>(2);
        final Storage.Index index = this.storage.getIndex(operation.getId());
        if (index == null) {
            // やるだけ無駄なんで止める。
            LOG.log(Level.FINEST, "{0} の対象を保持していませんでした。", operation);
            return CheckOneDemandResult.newNoDemand();
        } else {
            mail.add(new CheckOneDemandMessage(new StockEntry(this.idRegistry, index.getId(), index.getDate(), index.getHashValue())));
        }
        mail.add(new SessionMessage(session));

        LOG.log(Level.FINEST, "{0} に {1} の需要を確認します。", new Object[] { operation.getDestination(), operation.getId() });

        // 送受信。
        this.network.sendMail(operation.getDestination(), ConnectionTypes.DATA, mail);
        final ReceivedMail receivedMail = this.sessionManager.waitReply(session, start + timeout - System.currentTimeMillis());

        if (receivedMail == null) {
            if (start + timeout <= System.currentTimeMillis()) {
                LOG.log(Level.FINEST, "{0} は時間切れになりました。", operation);
                // 非リレーの時間切れは懲罰対象。
                this.network.removeLostPeer(operation.getDestination());
                ConcurrentFunctions.completePut(new OutlawReport(operation.getDestination()), this.outlawReportSink);
            } else {
                // 通信異常。
                // エラー報告が別に上がっているはずなので、ここで個体の削除はしない。
                LOG.log(Level.FINER, "なぜか {0} は失敗しました。", operation);
            }
            return CheckOneDemandResult.newGiveUp();
        } else {
            if (receivedMail.getMail().get(0) instanceof CheckOneDemandReply) {
                // 通信網の更新。
                LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { operation.getDestination(), operation });
                this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                final CheckOneDemandReply reply = (CheckOneDemandReply) receivedMail.getMail().get(0);
                if (reply.isRejected()) {
                    LOG.log(Level.FINEST, "{0} は断られました。", operation);
                    return CheckOneDemandResult.newGiveUp();
                } else if (reply.isGivenUp()) {
                    LOG.log(Level.FINEST, "{0} は諦められました。", operation);
                    return CheckOneDemandResult.newGiveUp();
                } else if (reply.hasNoDemand()) {
                    LOG.log(Level.FINEST, "{0} は無用でした。", operation);
                    return CheckOneDemandResult.newNoDemand();
                } else {
                    LOG.log(Level.FINEST, "{0} が成功しました。", operation);
                    return new CheckOneDemandResult(reply.getDemand());
                }
            } else {
                // プロトコル違反。
                LOG.log(Level.WARNING, "{0} からの返事の型 {1} は期待する型 {2} と異なります。", new Object[] { operation.getDestination(),
                        receivedMail.getMail().get(0).getClass(), CheckOneDemandReply.class });
                ConcurrentFunctions.completePut(new OutlawReport(operation.getDestination()), this.outlawReportSink);
                return CheckOneDemandResult.newGiveUp();
            }
        }
    }
}
