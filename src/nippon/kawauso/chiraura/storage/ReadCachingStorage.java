package nippon.kawauso.chiraura.storage;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * データ片をキャッシュして read の性能を向上させる倉庫。
 * @author chirauraNoSakusha
 */
final class ReadCachingStorage implements Storage {

    /*
     * base と cache はそれぞれ内部で同期されているので、単独で使う場合には lock の必要は無い。
     * cache を書き換える場合にのみ、base の内容と同期するために lock する。
     */
    private final Storage base;
    private final Map<Chunk.Id<?>, Chunk> cache;

    @SuppressWarnings("serial")
    ReadCachingStorage(final Storage base, final int capacity) {
        if (base == null) {
            throw new IllegalArgumentException("Null base storage.");
        }
        this.base = base;
        this.cache = Collections.synchronizedMap(
                new LinkedHashMap<Chunk.Id<?>, Chunk>(16, 0.75F, true) {
                    @Override
                    protected boolean removeEldestEntry(final Map.Entry<Chunk.Id<?>, Chunk> eldest) {
                        return this.size() > capacity;
                    }
                });
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
    public boolean contains(final Chunk.Id<?> id) throws IOException, InterruptedException {
        if (this.cache.containsKey(id)) {
            return true;
        } else {
            return this.base.contains(id);
        }
    }

    @Override
    public Storage.Index getIndex(final Chunk.Id<?> id) throws MyRuleException, IOException, InterruptedException {
        final Chunk chunk = this.cache.get(id);
        if (chunk == null) {
            return this.base.getIndex(id);
        } else {
            return new SimpleIndex(chunk);
        }
    }

    @Override
    public Map<Chunk.Id<?>, Storage.Index> getIndices(final Address min, final Address max) throws IOException, InterruptedException {
        return this.base.getIndices(min, max);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Chunk> T read(final Chunk.Id<T> id) throws MyRuleException, IOException, InterruptedException {
        Chunk chunk = this.cache.get(id);
        if (chunk != null) {
            // キャッシュしてた。
            return (T) chunk;
        } else if (!this.base.contains(id)) {
            // 無い。
            return null;
        }

        this.base.lock(id); // キャッシュ書き換えのためのロック。
        try {
            // 同時にロックを試みた場合に下位層の読み込みを減らすためにダブルチェック。
            chunk = this.cache.get(id);
            if (chunk != null) {
                // ついさっきキャッシュしてた。
                return (T) chunk;
            }

            chunk = this.base.read(id);
            if (chunk == null) {
                // 無い。
                return null;
            }

            this.cache.put(chunk.getId(), chunk);
            return (T) chunk;
        } finally {
            this.base.unlock(id);
        }
    }

    @Override
    public boolean write(final Chunk chunk) throws IOException, InterruptedException {
        this.base.lock(chunk.getId()); // キャッシュと下位層の書き換えのためのロック。
        try {
            // 古いのを使い続ける可能性があるのでここで put はしない。
            final Chunk old = this.cache.get(chunk.getId());
            if (old != null) {
                if (chunk.equals(old)) {
                    // 保存してあるのと同じ。
                    return false;
                } else {
                    // 保存してあるのと違う。
                    this.base.forceWrite(chunk);
                    this.cache.put(chunk.getId(), chunk);
                    return true;
                }
            }

            if (this.base.write(chunk)) {
                this.cache.put(chunk.getId(), chunk);
                return true;
            } else {
                return false;
            }
        } finally {
            this.base.unlock(chunk.getId());
        }
    }

    @Override
    public void forceWrite(final Chunk chunk) throws IOException, InterruptedException {
        this.base.lock(chunk.getId()); // キャッシュと下位層の書き換えのためのロック。
        try {
            // 古いのを使い続ける可能性があるのでここで put はしない。
            final Chunk old = this.cache.get(chunk.getId());
            if (!chunk.equals(old)) {
                this.cache.put(chunk.getId(), chunk);
                this.base.forceWrite(chunk);
            }
        } finally {
            this.base.unlock(chunk.getId());
        }
    }

    @Override
    public boolean delete(final Chunk.Id<?> id) throws IOException, InterruptedException {
        boolean removed;
        this.base.lock(id); // キャッシュと下位層の書き換えのためのロック。
        try {
            removed = (this.cache.remove(id) != null);
            removed |= this.base.delete(id);
        } finally {
            this.base.unlock(id);
        }
        return removed;
    }

    @Override
    public void close() throws MyRuleException, InterruptedException, IOException {
        this.base.close();
    }

}
