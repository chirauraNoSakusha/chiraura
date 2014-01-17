/**
 * 
 */
package nippon.kawauso.chiraura.storage;

import java.io.IOException;
import java.util.Map;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * データ片の概要を別に管理して性能を向上させる倉庫。
 * @author chirauraNoSakusha
 */
final class IndexingStorage implements Storage {

    /*
     * base と cache はそれぞれ内部で同期されているので、単独で使う場合には lock の必要は無い。
     * cache を書き換える場合にのみ、base の内容と同期するために lock する。
     */
    private final Storage base;
    private final IndexCache cache;

    IndexingStorage(final Storage base, final int capacity) {
        if (base == null) {
            throw new IllegalArgumentException("Null base storage.");
        }
        this.base = base;
        this.cache = new IndexCache(capacity);
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
        IndexCache.Entry entry = this.cache.get(id);
        if (entry != null) {
            return entry.exists();
        }

        this.base.lock(id);
        try {
            entry = this.cache.get(id);
            if (entry != null) {
                return entry.exists();
            }
            if (this.base.contains(id)) {
                this.cache.put(id, IndexCache.Entry.newNotLoaded());
                return true;
            } else {
                this.cache.put(id, IndexCache.Entry.newNotExists());
                return false;
            }
        } finally {
            this.base.unlock(id);
        }
    }

    @Override
    public Index getIndex(final Chunk.Id<?> id) throws MyRuleException, IOException, InterruptedException {
        IndexCache.Entry entry = this.cache.get(id);
        if (entry != null && entry.isLoaded()) {
            return entry.getIndex();
        }

        this.base.lock(id);
        try {
            entry = this.cache.get(id);
            if (entry != null && entry.isLoaded()) {
                return entry.getIndex();
            }
            final Storage.Index index = this.base.getIndex(id);
            if (index != null) {
                this.cache.put(id, new IndexCache.Entry(index));
            } else {
                this.cache.put(id, IndexCache.Entry.newNotExists());
            }
            return index;
        } finally {
            this.base.unlock(id);
        }
    }

    @Override
    public Map<Chunk.Id<?>, Storage.Index> getIndices(final Address min, final Address max) throws IOException, InterruptedException {
        return this.base.getIndices(min, max);
    }

    @Override
    public <T extends Chunk> T read(final Chunk.Id<T> id) throws MyRuleException, IOException, InterruptedException {
        IndexCache.Entry entry = this.cache.get(id);
        if (entry != null && !entry.exists()) {
            return null;
        }

        this.base.lock(id);
        try {
            entry = this.cache.get(id);
            if (entry != null && !entry.exists()) {
                return null;
            }
            final T chunk = this.base.read(id);
            if (chunk == null) {
                this.cache.put(id, IndexCache.Entry.newNotExists());
            } else {
                this.cache.put(id, new IndexCache.Entry(new SimpleIndex(chunk)));
            }
            return chunk;
        } finally {
            this.base.unlock(id);
        }
    }

    @Override
    public boolean write(final Chunk chunk) throws IOException, InterruptedException {
        this.base.lock(chunk.getId());
        try {
            final IndexCache.Entry entry = this.cache.get(chunk.getId());
            if (entry != null && entry.isLoaded()) {
                final Storage.Index index = new SimpleIndex(chunk);
                if (entry.getIndex().equals(index)) {
                    // TODO 本当に同じとみなして良いか？一応ハッシュ値も比べてる。
                    return false;
                } else {
                    // 保存してあるのと違う。
                    this.base.forceWrite(chunk);
                    this.cache.put(chunk.getId(), new IndexCache.Entry(index));
                    return true;
                }
            }

            if (this.base.write(chunk)) {
                this.cache.put(chunk.getId(), new IndexCache.Entry(new SimpleIndex(chunk)));
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
        this.base.lock(chunk.getId());
        try {
            this.base.forceWrite(chunk);
            this.cache.put(chunk.getId(), new IndexCache.Entry(new SimpleIndex(chunk)));
        } finally {
            this.base.unlock(chunk.getId());
        }
    }

    @Override
    public boolean delete(final Chunk.Id<?> id) throws IOException, InterruptedException {
        IndexCache.Entry entry = this.cache.get(id);
        if (entry != null && !entry.exists()) {
            return false;
        }

        this.base.lock(id);
        try {
            entry = this.cache.get(id);
            if (entry != null && !entry.exists()) {
                return false;
            }
            final boolean result = this.base.delete(id);
            this.cache.put(id, IndexCache.Entry.newNotExists());
            return result;
        } finally {
            this.base.unlock(id);
        }
    }

    @Override
    public void close() throws MyRuleException, InterruptedException, IOException {
        this.base.close();
    }

}
