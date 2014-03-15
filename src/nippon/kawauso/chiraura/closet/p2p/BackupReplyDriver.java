package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class BackupReplyDriver {

    private static final Logger LOG = Logger.getLogger(BackupReplyDriver.class.getName());

    private final NetworkWrapper network;
    private final StorageWrapper storage;

    BackupReplyDriver(final NetworkWrapper network, final StorageWrapper storage) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        }
        this.network = network;
        this.storage = storage;
    }

    void execute(final BackupReply reply) throws InterruptedException {
        if (reply.isRejected() || reply.isGivenUp() || reply.isSuccess()) {
            LOG.log(Level.FINEST, "{0} に対してすることはありません。", reply);
        } else {
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
        }
    }

}
