/**
 * 
 */
package nippon.kawauso.chiraura.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
final class RangeIndexingStorage implements Storage {

    private final Storage base;
    private final RangeIndexCache cache;

    /*
     * 並列利用する場合、キャッシュは正確ではない。
     * なので、操作の度にちょっとずつ修正する。
     */

    /**
     * 作成する。
     * @param base 基礎にする倉庫
     * @param capacity キャッシュの容量 (キャッシュするパターン数)
     */
    RangeIndexingStorage(final Storage base, final int capacity) {
        this.base = base;
        this.cache = new RangeIndexCache(capacity);
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
        if (this.base.contains(id)) {
            return true;
        } else {
            // キャッシュの修正。
            this.cache.removeIndex(id);
            return false;
        }
    }

    @Override
    public Storage.Index getIndex(final Chunk.Id<?> id) throws MyRuleException, IOException, InterruptedException {
        final Storage.Index index = this.base.getIndex(id);

        // キャッシュの修正。
        if (index == null) {
            this.cache.removeIndex(id);
        } else {
            this.cache.addIndex(index);
        }

        return index;
    }

    // private int hit = 0;
    // private int miss = 0;

    @Override
    public Map<Chunk.Id<?>, Storage.Index> getIndices(final Address start, final Address end) throws IOException, InterruptedException {
        Map<Chunk.Id<?>, Storage.Index> indices = this.cache.getIndices(start, end);
        // System.out.println("hit " + this.hit + " miss " + this.miss);
        if (indices != null) {
            // this.hit++;
            return indices;
        } else {
            // this.miss++;
            indices = this.base.getIndices(start, end);
            this.cache.add(start, end, new HashMap<>(indices)); // 防御的複製。
            return indices;
        }
    }

    @Override
    public <T extends Chunk> T read(final Chunk.Id<T> id) throws MyRuleException, IOException, InterruptedException {
        final T chunk = this.base.read(id);

        // キャッシュの修正。
        if (chunk == null) {
            this.cache.removeIndex(id);
        } else {
            this.cache.addIndex(new SimpleIndex(chunk));
        }

        return chunk;
    }

    @Override
    public boolean write(final Chunk chunk) throws IOException, InterruptedException {
        this.base.lock(chunk.getId());
        try {
            // キャッシュの修正。
            this.cache.addIndex(new SimpleIndex(chunk));
            if (this.base.write(chunk)) {
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

            // キャッシュの修正。
            this.cache.addIndex(new SimpleIndex(chunk));
        } finally {
            this.base.unlock(chunk.getId());
        }
    }

    @Override
    public boolean delete(final Chunk.Id<?> id) throws IOException, InterruptedException {
        this.base.lock(id);
        try {
            final boolean result = this.base.delete(id);

            // キャッシュの修正。
            this.cache.removeIndex(id);

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
