package nippon.kawauso.chiraura.storage;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
final class WriteCachingStorage implements Storage {

    private static final Logger LOG = Logger.getLogger(WriteCachingStorage.class.getName());
    /*
     * base と cache を単独で使う場合には lock の必要は無い。
     * cache を書き換える場合にのみ、base の内容と同期するために lock する。
     */
    private final Storage base;
    private final int capacity;
    private final WriteCache cache;

    WriteCachingStorage(final Storage base, final int capacity) {
        if (base == null) {
            throw new IllegalArgumentException("Null base storage.");
        }
        this.base = base;
        this.capacity = capacity;
        this.cache = new WriteCache(base.getChunkRegistry());
    }

    @Override
    public <C extends Chunk, I extends Chunk.Id<C>> void registerChunk(final long type, final Class<C> chunkClass,
            final BytesConvertible.Parser<? extends C> chunkParser, final Class<I> idClass, final BytesConvertible.Parser<? extends I> idParser) {
        this.base.registerChunk(type, chunkClass, chunkParser, idClass, idParser);
    }

    @Override
    public TypeRegistry<Chunk> getChunkRegistry() {
        return this.base.getChunkRegistry();
    }

    @Override
    public TypeRegistry<Chunk.Id<?>> getIdRegistry() {
        return this.base.getIdRegistry();
    }

    @Override
    public void lock(final Chunk.Id<?> id) throws InterruptedException {
        this.base.lock(id);
    }

    @Override
    public boolean tryLock(final Chunk.Id<?> id) {
        return this.base.tryLock(id);
    }

    @Override
    public void unlock(final Chunk.Id<?> id) {
        this.base.unlock(id);
    }

    @Override
    public boolean contains(final Chunk.Id<?> id) throws InterruptedException, IOException {
        if (this.cache.contains(id)) {
            return true;
        } else {
            return this.base.contains(id);
        }
    }

    @Override
    public Index getIndex(final Chunk.Id<?> id) throws MyRuleException, IOException, InterruptedException {
        final WriteCache.Entry entry = this.cache.get(id);
        if (entry == null) {
            return this.base.getIndex(id);
        } else {
            return new SimpleIndex(entry.getChunk());
        }
    }

    @Override
    public Map<Chunk.Id<?>, Storage.Index> getIndices(final Address min, final Address max) throws IOException, InterruptedException {
        final Map<Chunk.Id<?>, Storage.Index> indices = this.base.getIndices(min, max);
        final Map<Chunk.Id<?>, Storage.Index> cachedIndices = this.cache.getIndices(min, max);
        indices.putAll(cachedIndices);
        return indices;
    }

    /**
     * キャッシュのサイズを容量以下に直す。
     * @throws IOException 書き込みエラー
     * @throws InterruptedException 割り込みエラー
     */
    private void trimCache() throws IOException, InterruptedException {
        for (WriteCache.Entry eldest; this.capacity < this.cache.size() && (eldest = this.cache.getEldest()) != null;) {
            if (!this.base.tryLock(eldest.getChunk().getId())) {
                // ロックできなかったら諦める。
                break;
            }
            try {
                final WriteCache.Entry removed = this.cache.removeEldestIfOver(eldest.getChunk().getId(), this.capacity);
                if (removed != null && removed.isModified()) {
                    this.base.write(removed.getChunk());
                }
            } finally {
                this.base.unlock(eldest.getChunk().getId());
            }
        }
    }

    @Override
    public <T extends Chunk> T read(final Chunk.Id<T> id) throws MyRuleException, IOException, InterruptedException {
        final T chunk = subRead(id);
        trimCache();
        return chunk;
    }

    @SuppressWarnings("unchecked")
    private <T extends Chunk> T subRead(final Chunk.Id<T> id) throws MyRuleException, IOException, InterruptedException {
        WriteCache.Entry entry = this.cache.get(id);
        if (entry != null) {
            // キャッシュしてた。
            return (T) entry.getChunk();
        } else if (!this.base.contains(id)) {
            // 無い。
            return null;
        }

        this.base.lock(id); // キャッシュ書き換えのためのロック。
        try {
            entry = this.cache.get(id);
            if (entry != null) {
                // ついさっきキャッシュしてた。
                return (T) entry.getChunk();
            }

            final T chunk = this.base.read(id);
            if (chunk == null) {
                // 無い。
                return null;
            }
            this.cache.addNotModified(chunk);
            return chunk;
        } finally {
            this.base.unlock(id);
        }
    }

    @Override
    public boolean write(final Chunk chunk) throws IOException, InterruptedException {

        final boolean result;
        this.base.lock(chunk.getId());
        try {
            final WriteCache.Entry entry = this.cache.get(chunk.getId());
            if (entry != null) {
                if (entry.getChunk().equals(chunk)) {
                    // 保存されてるのと同じ。
                    return false;
                } else {
                    // 保存されてるのと違う。
                    this.cache.add(chunk);
                    return true;
                }
            }

            Chunk old = null;
            try {
                old = this.base.read(chunk.getId());
            } catch (final MyRuleException e) {
                LOG.log(Level.WARNING, "異常が発生しました", e);
                LOG.log(Level.INFO, "保存されていた {0} は壊れていました。", chunk.getId());
                /*
                 * 次からは chunk が使われるので放置で良い。
                 */
            }
            if (chunk.equals(old)) {
                // 下の層で使われてるかもしれないから古いの優先。
                this.cache.add(old);
                result = false;
            } else {
                this.cache.add(chunk);
                result = true;
            }
        } finally {
            this.base.unlock(chunk.getId());
        }

        trimCache();
        return result;
    }

    @Override
    public void forceWrite(final Chunk chunk) throws IOException, InterruptedException {
        final WriteCache.Entry old = this.cache.get(chunk.getId());
        if (old != null && old.getChunk().equals(chunk)) {
            // 同じのがある。
            return;
        }

        this.base.lock(chunk.getId());
        try {
            this.cache.add(chunk);
        } finally {
            this.base.unlock(chunk.getId());
        }

        trimCache();
    }

    @Override
    public boolean delete(final Chunk.Id<?> id) throws IOException, InterruptedException {
        boolean removed;
        this.base.lock(id);
        try {
            removed = (this.cache.remove(id) != null);
            removed |= this.base.delete(id);
        } finally {
            this.base.unlock(id);
        }
        trimCache();
        return removed;
    }

    @Override
    public void close() throws IOException, MyRuleException, InterruptedException {
        for (WriteCache.Entry eldest; (eldest = this.cache.getEldest()) != null;) {
            try {
                this.base.lock(eldest.getChunk().getId());
                try {
                    final WriteCache.Entry removed = this.cache.removeEldestIfOver(eldest.getChunk().getId(), 0);
                    if (removed != null && removed.isModified()) {
                        this.base.write(removed.getChunk());
                    }
                } finally {
                    this.base.unlock(eldest.getChunk().getId());
                }
            } catch (final InterruptedException ignored) {
            }
        }

        this.base.close();
    }
}
