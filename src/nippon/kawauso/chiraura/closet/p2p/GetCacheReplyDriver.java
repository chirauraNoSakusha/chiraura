/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class GetCacheReplyDriver {

    private static final Logger LOG = Logger.getLogger(GetCacheReplyDriver.class.getName());

    // 参照。
    private final StorageWrapper storage;

    GetCacheReplyDriver(final StorageWrapper storage) {
        if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        }
        this.storage = storage;
    }

    void execute(final GetCacheReply reply) throws InterruptedException {
        if (reply.isRejected() || reply.isGivenUp()) {
            LOG.log(Level.FINEST, "{0} に対してすることはありません。", reply);
        } else if (reply.isNotFound()) {
            StorageWrapper.CacheResult<?> cache = null;
            try {
                cache = this.storage.addNotFoundCache(reply.getId(), reply.getAccessDate());
            } catch (final IOException e) {
                LOG.log(Level.FINEST, "{0} の情報更新中に異常が発生しました。", reply.getChunk().getId());
            }
            if (cache != null && cache.isNotFound()) {
                LOG.log(Level.FINEST, "{0} により更新しました。", reply);
            } else {
                LOG.log(Level.FINEST, "{0} による更新はありませんでした。", reply);
            }
        } else {
            StorageWrapper.CacheResult<?> cache = null;
            try {
                cache = this.storage.forceWriteCache(reply.getChunk(), reply.getAccessDate());
            } catch (final IOException e) {
                LOG.log(Level.FINEST, "{0} の書き込みに異常が発生しました。", reply.getChunk().getId());
            }
            if (cache != null && cache.isSuccess()) {
                LOG.log(Level.FINEST, "{0} により更新しました。", reply);
            } else {
                LOG.log(Level.FINEST, "{0} による更新はありませんでした。", reply);
            }
        }
    }

}
