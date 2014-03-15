package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class PatchOrAddAndGetCacheReplyDriver {

    private static final Logger LOG = Logger.getLogger(PatchOrAddAndGetCacheReplyDriver.class.getName());

    // 参照。
    private final StorageWrapper storage;

    PatchOrAddAndGetCacheReplyDriver(final StorageWrapper storage) {
        if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        }
        this.storage = storage;
    }

    void execute(final PatchOrAddAndGetCacheReply reply) throws InterruptedException {
        if (reply.isRejected() || reply.isGivenUp()) {
            LOG.log(Level.FINEST, "{0} に対してすることはありません。", reply);
        } else {
            StorageWrapper.CacheResult<?> cache = null;
            try {
                cache = this.storage.forceWriteCache(reply.getChunk(), reply.getAccessDate());
            } catch (final IOException e) {
                LOG.log(Level.FINEST, "{0} の書き込み中に異常が発生しました。", reply.getChunk().getId());
            }
            if (cache != null && cache.isSuccess()) {
                LOG.log(Level.FINEST, "{0} により更新されました。", reply);
            } else {
                LOG.log(Level.FINEST, "{0} による更新はありませんでした。", reply);
            }
        }
    }

}
