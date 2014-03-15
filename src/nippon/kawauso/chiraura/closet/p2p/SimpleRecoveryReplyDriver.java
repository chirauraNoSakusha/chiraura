package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.closet.Mountain;

/**
 * @author chirauraNoSakusha
 */
final class SimpleRecoveryReplyDriver {
    private static final Logger LOG = Logger.getLogger(SimpleRecoveryReplyDriver.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final StorageWrapper storage;

    SimpleRecoveryReplyDriver(final NetworkWrapper network, final StorageWrapper storage) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        }
        this.network = network;
        this.storage = storage;
    }

    <T extends Mountain> void execute(final SimpleRecoveryReply reply) throws InterruptedException {
        if (reply.isRejected() || reply.isNotFound()) {
            LOG.log(Level.FINEST, "{0} に対してすることはありません。", reply);
        } else {
            if (!this.network.dominates(reply.getChunk().getId().getAddress())) {
                LOG.log(Level.FINEST, "{0} は既に自分の管轄ではありません。", reply);
            } else {
                boolean result = false;
                try {
                    result = this.storage.weakWrite(reply.getChunk());
                } catch (final IOException e) {
                    LOG.log(Level.FINEST, "{0} の更新中に異常が発生しました。", reply.getChunk().getId());
                }
                if (result) {
                    LOG.log(Level.FINEST, "{0} による更新が成功しました。", reply);
                } else {
                    LOG.log(Level.FINEST, "{0} による更新は必要ありませんでした。", reply);
                }
            }
        }
    }

}
