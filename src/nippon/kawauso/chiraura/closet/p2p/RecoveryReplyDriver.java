/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Mountain;

/**
 * @author chirauraNoSakusha
 */
final class RecoveryReplyDriver {

    private static final Logger LOG = Logger.getLogger(RecoveryReplyDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;

    RecoveryReplyDriver(final NetworkWrapper network, final StorageWrapper storage) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        }
        this.network = network;
        this.storage = storage;
    }

    <T extends Mountain> void execute(final RecoveryReply<T> reply) throws InterruptedException {
        if (reply.isRejected() || reply.isNotFound()) {
            LOG.log(Level.FINEST, "{0} に対してすることはありません。", reply);
        } else if (reply.isGet()) {
            if (!this.network.dominates(reply.getChunk().getId().getAddress())) {
                LOG.log(Level.FINEST, "{0} は既に自分の管轄ではありません。", reply);
            } else {
                StorageWrapper.Result<?> result = null;
                try {
                    result = this.storage.update(reply.getChunk());
                } catch (final IOException e) {
                    LOG.log(Level.FINEST, "{0} の更新中に異常が発生しました。", reply.getChunk().getId());
                }
                if (result != null && result.getChunk().equals(reply.getChunk())) {
                    LOG.log(Level.FINEST, "{0} による更新が成功しました。", reply);
                } else {
                    LOG.log(Level.FINEST, "{0} による更新は失敗しました。", reply);
                }
            }
        } else {
            if (!this.network.dominates(reply.getId().getAddress())) {
                LOG.log(Level.FINEST, "{0} は既に自分の管轄ではありません。", reply);
            } else {
                StorageWrapper.Result<T> result = null;
                try {
                    result = this.storage.patch(reply.getId(), reply.getDiffs());
                } catch (final IOException e) {
                    LOG.log(Level.FINEST, "{0} の更新中に異常が発生しました。", reply.getChunk().getId());
                }
                if (result != null && !result.isNotFound() && result.getChunk().getHashValue().equals(reply.getHashValue())) {
                    LOG.log(Level.FINEST, "{0} が差分で成功しました。", reply);
                } else {
                    LOG.log(Level.FINEST, "{0} は差分で不完全に終わりました。", reply);
                }
            }
        }
    }

}
