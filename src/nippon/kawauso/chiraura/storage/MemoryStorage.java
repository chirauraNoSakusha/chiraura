package nippon.kawauso.chiraura.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;

/**
 * ReadCachingStorage や IndexingStorage のテスト用に使う基本データ片倉庫。
 * 並列対応。
 * @author chirauraNoSakusha
 */
final class MemoryStorage implements Storage {

    private final LockPool<Chunk.Id<?>> locks;
    private final TypeRegistry<Chunk> chunkRegistry;
    private final TypeRegistry<Chunk.Id<?>> idRegistry;

    private final Map<Chunk.Id<?>, Chunk> idToChunk;
    private final NavigableMap<Address, Chunk> addressToChunk;

    MemoryStorage() {
        this.locks = new LockPool<>();
        this.chunkRegistry = TypeRegistries.newRegistry();
        this.idRegistry = TypeRegistries.newRegistry();

        this.idToChunk = new ConcurrentHashMap<>();
        this.addressToChunk = new ConcurrentSkipListMap<>();
    }

    @Override
    public <C extends Chunk, I extends Chunk.Id<C>> void registerChunk(final long type, final Class<C> chunkClass,
            final BytesConvertible.Parser<? extends C> chunkParser, final Class<I> idClass, final BytesConvertible.Parser<? extends I> idParser) {
        this.chunkRegistry.register(type, chunkClass, chunkParser);
        this.idRegistry.register(type, idClass, idParser);
    }

    @Override
    public TypeRegistry<Chunk> getChunkRegistry() {
        return TypeRegistries.unregisterableRegistry(this.chunkRegistry);
    }

    @Override
    public TypeRegistry<Chunk.Id<?>> getIdRegistry() {
        return TypeRegistries.unregisterableRegistry(this.idRegistry);
    }

    @Override
    public void lock(final Chunk.Id<?> id) throws InterruptedException {
        this.locks.lock(id);
    }

    @Override
    public boolean tryLock(final Chunk.Id<?> id) {
        return this.locks.tryLock(id);
    }

    @Override
    public void unlock(final Chunk.Id<?> id) {
        this.locks.unlock(id);
    }

    @Override
    public boolean contains(final Chunk.Id<?> id) throws InterruptedException {
        this.locks.lock(id);
        try {
            return this.idToChunk.containsKey(id);
        } finally {
            this.locks.unlock(id);
        }
    }

    @Override
    public Storage.Index getIndex(final Chunk.Id<?> id) {
        final Chunk chunk = this.idToChunk.get(id);
        if (chunk == null) {
            return null;
        } else {
            return new SimpleIndex(chunk);
        }
    }

    @Override
    public Map<Chunk.Id<?>, Storage.Index> getIndices(final Address min, final Address max) {
        final Map<Chunk.Id<?>, Storage.Index> indices = new HashMap<>();
        for (final Chunk chunk : this.addressToChunk.subMap(min, true, max, true).values()) {
            indices.put(chunk.getId(), new SimpleIndex(chunk));
        }
        return indices;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Chunk> T read(final Chunk.Id<T> id) {
        return (T) this.idToChunk.get(id);
    }

    @Override
    public boolean write(final Chunk chunk) throws InterruptedException {
        this.locks.lock(chunk.getId());
        try {
            final Chunk old = this.idToChunk.get(chunk.getId());
            if (chunk.equals(old)) {
                return false;
            } else {
                this.idToChunk.put(chunk.getId(), chunk);
                this.addressToChunk.put(chunk.getId().getAddress(), chunk);
                return true;
            }
        } finally {
            this.locks.unlock(chunk.getId());
        }
    }

    @Override
    public void forceWrite(final Chunk chunk) throws InterruptedException {
        this.locks.lock(chunk.getId());
        try {
            this.idToChunk.put(chunk.getId(), chunk);
            this.addressToChunk.put(chunk.getId().getAddress(), chunk);
        } finally {
            this.locks.unlock(chunk.getId());
        }
    }

    @Override
    public boolean delete(final Chunk.Id<?> id) throws InterruptedException {
        this.locks.lock(id);
        try {
            if (this.idToChunk.remove(id) != null) {
                this.addressToChunk.remove(id.getAddress());
                return true;
            } else {
                return false;
            }
        } finally {
            this.locks.unlock(id);
        }
    }

    @Override
    public void close() {}

}
