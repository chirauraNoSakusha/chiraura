package nippon.kawauso.chiraura.storage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.container.BasicInputOrderedMap;
import nippon.kawauso.chiraura.lib.container.InputOrderedMap;

/**
 * @author chirauraNoSakusha
 */
final class RangeIndexCache {

    /*
     * 範囲と概要を結び付けて保存する。
     * 保存数を制限し、古いものは消す。
     * 範囲が重複する場合は、古い方を消す。
     */

    private static final class Range {
        private final Address start;
        private final Address end;

        private Range(final Address start, final Address end) {
            if (start == null) {
                throw new IllegalArgumentException("Null start.");
            } else if (end == null) {
                throw new IllegalArgumentException("Null end.");
            }
            this.start = start;
            this.end = end;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            return prime * this.start.hashCode() + this.end.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof Range)) {
                return false;
            }
            final Range other = (Range) obj;
            return this.end.equals(other.end) && this.start.equals(other.start);
        }
    }

    private final int limit;
    private final InputOrderedMap<Range, Map<Chunk.Id<?>, Storage.Index>> rangeToIndices;

    // 範囲の先頭から末尾への写像。
    private final NavigableMap<Address, Address> ranges;

    RangeIndexCache(final int limit) {
        this.limit = limit;
        this.rangeToIndices = new BasicInputOrderedMap<>();
        this.ranges = new TreeMap<>();
    }

    /**
     * データ片の概要を列挙する。
     * キャッシュの読み込み操作。
     * @param start 列挙するデータ片の論理位置の先頭
     * @param end 列挙するデータ片の論理位置の末尾
     * @return データ片の概要。
     *         キャッシュされていない場合は null
     */
    synchronized Map<Chunk.Id<?>, Storage.Index> getIndices(final Address start, final Address end) {
        final Map<Chunk.Id<?>, Storage.Index> indices = this.rangeToIndices.get(new Range(start, end));
        if (indices == null) {
            return null;
        } else {
            return new HashMap<>(indices);
        }
    }

    private void trim() {
        while (this.limit < this.ranges.size()) {
            final Map.Entry<Range, ?> entry = this.rangeToIndices.removeEldest();
            this.ranges.remove(entry.getKey().start);
        }
    }

    /**
     * データ片の概要を加える。
     * キャッシュする操作。
     * @param start 結び付ける論理位置の先頭
     * @param end 結び付ける論理位置の末尾
     * @param indices データ片の概要
     */
    synchronized void add(final Address start, final Address end, final Map<Chunk.Id<?>, Storage.Index> indices) {
        final Range range = new Range(start, end);
        if (this.rangeToIndices.containsKey(range)) {
            // 上書き。
            this.rangeToIndices.put(range, indices);
        } else {
            // 新規。

            // 重複削除。
            final Map.Entry<Address, Address> lowerMax = this.ranges.lowerEntry(start);
            if (lowerMax != null && start.compareTo(lowerMax.getValue()) <= 0) {
                // 前の範囲の一部と重複。
                this.rangeToIndices.remove(new Range(lowerMax.getKey(), lowerMax.getValue()));
                this.ranges.remove(lowerMax.getKey());
            }
            for (final Iterator<Map.Entry<Address, Address>> iterator = this.ranges.subMap(start, true, end, true).entrySet().iterator(); iterator.hasNext();) {
                final Map.Entry<Address, Address> entry = iterator.next();
                this.rangeToIndices.remove(new Range(entry.getKey(), entry.getValue()));
                iterator.remove();
            }

            // 登録。
            this.rangeToIndices.put(range, indices);
            this.ranges.put(start, end);
        }
        trim();
    }

    /**
     * データ片の概要の 1 つを削除する。
     * キャッシュの修正操作。
     * @param id 削除するデータ片の識別子
     */
    synchronized void removeIndex(final Chunk.Id<?> id) {
        final Map.Entry<Address, Address> range = this.ranges.floorEntry(id.getAddress());
        if (range != null && id.getAddress().compareTo(range.getValue()) <= 0) {
            // 含む範囲があった。
            this.rangeToIndices.get(new Range(range.getKey(), range.getValue())).remove(id);
        }
    }

    /**
     * データ片の概要を加える。
     * キャッシュの修正操作。
     * @param index 加える概要
     */
    synchronized void addIndex(final Storage.Index index) {
        final Map.Entry<Address, Address> range = this.ranges.floorEntry(index.getId().getAddress());
        if (range != null && index.getId().getAddress().compareTo(range.getValue()) <= 0) {
            // 含む範囲があった。
            this.rangeToIndices.get(new Range(range.getKey(), range.getValue())).put(index.getId(), index);
        }
    }

}
