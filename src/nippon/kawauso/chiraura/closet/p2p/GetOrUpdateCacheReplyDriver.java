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
final class GetOrUpdateCacheReplyDriver {

    private static final Logger LOG = Logger.getLogger(GetOrUpdateCacheReplyDriver.class.getName());

    // 参照。
    private final StorageWrapper storage;

    GetOrUpdateCacheReplyDriver(final StorageWrapper storage) {
        if (storage == null) {
            throw new IllegalArgumentException("Null storage.");
        }
        this.storage = storage;
    }

    <T extends Mountain> void execute(final GetOrUpdateCacheReply<T> reply) throws InterruptedException {
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
        } else if (reply.isGet()) {
            StorageWrapper.CacheResult<?> cache = null;
            try {
                cache = this.storage.forceWriteCache(reply.getChunk(), reply.getAccessDate());
            } catch (final IOException e) {
                LOG.log(Level.FINEST, "{0} の書き込み中に異常が発生しました。", reply.getChunk().getId());
            }
            if (cache != null && cache.isSuccess()) {
                LOG.log(Level.FINEST, "{0} により更新しました。", reply);
            } else {
                LOG.log(Level.FINEST, "{0} による更新はありませんでした。", reply);
            }
        } else {
            StorageWrapper.CacheResult<?> cache = null;
            try {
                cache = this.storage.patchCache(reply.getId(), reply.getDiffs(), reply.getHashValue(), reply.getAccessDate());
            } catch (final IOException e) {
                LOG.log(Level.FINEST, "{0} の更新中に異常が発生しました。", reply.getChunk().getId());
            }
            if (cache != null && !cache.isNotFound() && cache.isSuccess()) {
                LOG.log(Level.FINEST, "{0} により更新しました。", reply);
            } else {
                LOG.log(Level.FINEST, "{0} による更新はありませんでした。", reply);
            }
        }
    }

}
