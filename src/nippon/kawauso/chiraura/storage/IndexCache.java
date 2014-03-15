package nippon.kawauso.chiraura.storage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * データ片の概要のキャッシュ。
 * @author chirauraNoSakusha
 */
final class IndexCache {

    /**
     * データ片の概要。
     * @author chirauraNoSakusha
     */
    static final class Entry {

        private final boolean exists;
        private final boolean loaded;
        private final Storage.Index index;

        private Entry(final boolean exists, final boolean loaded, final Storage.Index index) {
            this.exists = exists;
            this.loaded = loaded;
            this.index = index;
        }

        Entry(final Storage.Index index) {
            this(true, true, index);
        }

        static Entry newNotExists() {
            return new Entry(false, false, null);
        }

        static Entry newNotLoaded() {
            return new Entry(true, false, null);
        }

        boolean exists() {
            return this.exists;
        }

        boolean isLoaded() {
            return this.loaded;
        }

        Storage.Index getIndex() {
            return this.index;
        }
    }

    /*
     * 新しく使用した順で固定数キャッシュする。
     */

    private final Map<Chunk.Id<?>, Entry> container;

    @SuppressWarnings("serial")
    IndexCache(final int capacity) {
        this.container = Collections.synchronizedMap(
                new LinkedHashMap<Chunk.Id<?>, Entry>(16, 0.75F, true) {
                    @Override
                    protected boolean removeEldestEntry(final Map.Entry<Chunk.Id<?>, Entry> eldest) {
                        return this.size() > capacity;
                    }
                });
    }

    /**
     * キャッシュを参照する。
     * @param id データ片の識別子
     * @return 概要。
     *         キャッシュされていない場合は null
     */
    Entry get(final Chunk.Id<?> id) {
        return this.container.get(id);
    }

    /**
     * キャッシュに登録する。
     * @param id データ片の識別子
     * @param entry データ片の概要
     * @return 過去の概要
     */
    Entry put(final Chunk.Id<?> id, final Entry entry) {
        return this.container.put(id, entry);
    }

}
