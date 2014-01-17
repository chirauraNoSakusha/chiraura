/**
 * 
 */
package nippon.kawauso.chiraura.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.container.BasicInputOrderedMap;
import nippon.kawauso.chiraura.lib.container.InputOrderedMap;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;

/**
 * 書き込みキャッシュ。
 * @author chirauraNoSakusha
 */
final class WriteCache {

    static final class Entry {
        private final boolean modified;
        private final Chunk chunk;

        private Entry(final boolean written, final Chunk chunk) {
            this.modified = written;
            this.chunk = chunk;
        }

        boolean isModified() {
            return this.modified;
        }

        Chunk getChunk() {
            return this.chunk;
        }

    }

    private static final Class<? extends Chunk> MIN_TYPE = null;
    private static final Class<? extends Chunk> MAX_TYPE = (new Chunk() {
        @Override
        public int byteSize() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public int toStream(final OutputStream output) throws IOException {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public Chunk.Id<?> getId() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public long getDate() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public HashValue getHashValue() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }).getClass();

    private final class Key implements Comparable<Key> {

        private final Address address;
        private final Class<? extends Chunk> type;

        private Key(final Address address, final Class<? extends Chunk> type) {
            this.address = address;
            this.type = type;
        }

        private Key(final Chunk chunk) {
            this(chunk.getId().getAddress(), chunk.getClass());
        }

        private Key(final Chunk.Id<?> id) {
            this(id.getAddress(), id.getChunkClass());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.address.hashCode();
            result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof Key)) {
                return false;
            }
            final Key other = (Key) obj;
            return this.address.equals(other.address) && this.type == other.type;
        }

        @Override
        public int compareTo(final Key o) {
            final int result = this.address.compareTo(o.address);
            if (result != 0) {
                return result;
            } else if (this.type == o.type) {
                return 0;
            } else if (this.type == MIN_TYPE || o.type == MAX_TYPE) {
                return -1;
            } else if (this.type == MAX_TYPE || o.type == MIN_TYPE) {
                return 1;
            } else {
                final long v1 = WriteCache.this.registry.getId(this.type);
                final long v2 = WriteCache.this.registry.getId(o.type);
                if (v1 < v2) {
                    return 1;
                } else if (v1 > v2) {
                    return 1;
                } else {
                    // ちゃんと登録してあれば、ここには来ない。
                    return 0;
                }
            }
        }

    }

    private final InputOrderedMap<Chunk.Id<?>, Entry> idToEntry;
    private final TypeRegistry<Chunk> registry;
    private final ConcurrentNavigableMap<Key, Chunk> keyToChunk;

    WriteCache(final TypeRegistry<Chunk> registry) {
        this.idToEntry = new BasicInputOrderedMap<>();
        this.registry = registry;
        this.keyToChunk = new ConcurrentSkipListMap<>();
    }

    /**
     * キャッシュされているデータ片の数を返す。
     * @return キャッシュされているデータ片の数
     */
    synchronized int size() {
        return this.idToEntry.size();
    }

    /**
     * データ片がキャッシュされているかどうかの検査
     * @param id データ片の識別子
     * @return キャッシュされていれば true
     */
    synchronized boolean contains(final Chunk.Id<?> id) {
        return this.idToEntry.containsKey(id);
    }

    /**
     * キャッシュを参照する。
     * @param id データ片の識別子
     * @return データ片。
     *         キャッシュされていない場合は null
     */
    synchronized Entry get(final Chunk.Id<?> id) {
        // 順番を更新するため、削除と挿入を行う。
        final Entry entry = this.idToEntry.remove(id);
        if (entry == null) {
            return null;
        } else {
            this.idToEntry.put(entry.chunk.getId(), entry);
            return entry;
        }
    }

    /**
     * 一番古いデータ片を返す。
     * @return 一番古いデータ片。
     *         空の場合は null
     */
    synchronized Entry getEldest() {
        final Map.Entry<Chunk.Id<?>, Entry> eldest = this.idToEntry.getEldest();
        if (eldest != null) {
            return eldest.getValue();
        } else {
            return null;
        }
    }

    /**
     * 指定した識別子が一番古いデータ片の識別子であり、かつ、
     * 登録データ片の数が指定した数より多かったら、そのデータ片を削除する。
     * @param id 削除するデータ片の識別子
     * @param capacity 許容するデータ片の数
     * @return 削除したデータ片
     */
    synchronized Entry removeEldestIfOver(final Chunk.Id<?> id, final int capacity) {
        if (this.idToEntry.size() <= capacity) {
            return null;
        }

        final Map.Entry<Chunk.Id<?>, Entry> eldest = this.idToEntry.getEldest();
        if (eldest == null) {
            return null;
        }

        if (eldest.getKey().equals(id)) {
            this.idToEntry.removeEldest();
            this.keyToChunk.remove(new Key(id));
            return eldest.getValue();
        } else {
            return null;
        }
    }

    /**
     * 変更されていないデータ片をキャッシュする。
     * @param chunk データ片
     */
    synchronized void addNotModified(final Chunk chunk) {
        this.idToEntry.put(chunk.getId(), new Entry(false, chunk));
        this.keyToChunk.put(new Key(chunk), chunk);
    }

    /**
     * データ片をキャッシュする。
     * @param chunk データ片
     */
    synchronized void add(final Chunk chunk) {
        final Entry old = this.idToEntry.get(chunk.getId());
        if (old == null || !old.getChunk().equals(chunk)) {
            this.idToEntry.put(chunk.getId(), new Entry(true, chunk));
            this.keyToChunk.put(new Key(chunk), chunk);
        }
    }

    /**
     * キャッシュから消す。
     * @param id 消すデータ片の識別子
     * @return 消したデータ片
     */
    synchronized Entry remove(final Chunk.Id<?> id) {
        final Entry removed = this.idToEntry.remove(id);
        if (removed != null) {
            this.keyToChunk.remove(new Key(id)).getId();
            return removed;
        } else {
            return null;
        }
    }

    /**
     * 概要を列挙する。
     * @param min 列挙対象の最小論理位置
     * @param max 列挙対象の最大論理位置
     * @return 概要
     */
    Map<Chunk.Id<?>, Storage.Index> getIndices(final Address min, final Address max) {
        // 同期は keyToChunk の中で勝手にやられてる。
        final Map<Chunk.Id<?>, Storage.Index> indices = new HashMap<>();
        for (final Chunk chunk : this.keyToChunk.subMap(new Key(min, MIN_TYPE), false, new Key(max, MAX_TYPE), false).values()) {
            indices.put(chunk.getId(), new SimpleIndex(chunk));
        }
        return indices;
    }
}
