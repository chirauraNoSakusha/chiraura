/**
 * 
 */
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
final class CheckStockDriver {

    private static final Logger LOG = Logger.getLogger(CheckStockDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;
    private final SessionManager sessionManager;
    private final TypeRegistry<Chunk.Id<?>> idRegistry;

    private final int stockEntryLimit;

    CheckStockDriver(final NetworkWrapper network, final StorageWrapper storage, final SessionManager sessionManager,
            final TypeRegistry<Chunk.Id<?>> idRegistry, final int stockEntryLimit) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        } else if (sessionManager == null) {
            throw new IllegalArgumentException("Null session manager.");
        } else if (idRegistry == null) {
            throw new IllegalArgumentException("Null id registry.");
        } else if (stockEntryLimit < 0) {
            throw new IllegalArgumentException("Negative stock entry limit ( " + stockEntryLimit + " ).");
        }
        this.network = network;
        this.storage = storage;
        this.sessionManager = sessionManager;
        this.idRegistry = idRegistry;

        this.stockEntryLimit = stockEntryLimit;
    }

    CheckStockResult execute(final CheckStockOperation operation, final long timeout) throws InterruptedException, IOException {
        final long start = System.currentTimeMillis();

        // やりとりの準備。
        final Session session = this.sessionManager.newSession(operation.getDestination());

        // 手紙の準備。
        final List<Message> mail = new ArrayList<>(2);
        final Pair<Address, Address> domain = this.network.getDomain();
        final List<StockEntry> exclusive = StockEntry.getStockedEntries(this.storage, domain.getFirst(), domain.getSecond(), this.stockEntryLimit,
                new ArrayList<StockEntry>(0), this.idRegistry);
        mail.add(new CheckStockMessage(domain.getFirst(), domain.getSecond(), exclusive));
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
            return CheckStockResult.newGiveUp();
        } else {
            if (receivedMail.getMail().get(0) instanceof CheckStockReply) {
                // 通信網の更新。
                LOG.log(Level.FINEST, "{0} から {1} の結果が返ってきました。", new Object[] { operation.getDestination(), operation });
                this.network.addActivePeer(receivedMail.getSourceId(), receivedMail.getSourcePeer());
                final CheckStockReply reply = (CheckStockReply) receivedMail.getMail().get(0);
                if (reply.isRejected()) {
                    LOG.log(Level.FINEST, "{0} は断られました。", operation);
                    return CheckStockResult.newGiveUp();
                } else if (reply.isGivenUp()) {
                    LOG.log(Level.FINEST, "{0} は諦められました。", operation);
                    return CheckStockResult.newGiveUp();
                } else {
                    LOG.log(Level.FINEST, "{0} が成功しました。", operation);
                    return new CheckStockResult(reply.getStocks());
                }
            } else {
                // プロトコル違反。
                LOG.log(Level.WARNING, "{0} からの返信の型 {1} は期待する型 {2} と異なります。", new Object[] { operation.getDestination(),
                        receivedMail.getMail().get(0).getClass(), CheckStockReply.class });
                this.network.removeInvalidPeer(operation.getDestination());
                return CheckStockResult.newGiveUp();
            }
        }
    }

}
