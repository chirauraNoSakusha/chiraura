/**
 * 
 */
package nippon.kawauso.chiraura.lib.container;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 一番挿入が古い要素の削除が可能な順序マップ。
 * ただし、subMap() などで得られた順序マップに対する要素の追加や削除には対応しない。
 * それらを実行するとエラーを出さずに、間違った動作をするようになる。
 * @author chirauraNoSakusha
 * @param <K> キーのクラス
 * @param <V> 値のクラス
 */
public final class BasicInputOrderedNavigableMap<K, V> implements InputOrderedNavigableMap<K, V> {

    private final NavigableMap<K, V> container;
    private final InputOrderedMap<K, V> inputOrder;

    private BasicInputOrderedNavigableMap(final NavigableMap<K, V> container, final InputOrderedMap<K, V> inputOrder) {
        this.container = container;
        this.inputOrder = inputOrder;
    }

    /**
     * 作成する。
     */
    public BasicInputOrderedNavigableMap() {
        this(new TreeMap<K, V>(), new BasicInputOrderedMap<K, V>());
    }

    @Override
    public Comparator<? super K> comparator() {
        return this.container.comparator();
    }

    @Override
    public K firstKey() {
        return this.container.firstKey();
    }

    @Override
    public K lastKey() {
        return this.container.lastKey();
    }

    @Override
    public Set<K> keySet() {
        return this.container.keySet();
    }

    @Override
    public Collection<V> values() {
        return this.container.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return this.container.entrySet();
    }

    @Override
    public int size() {
        return this.container.size();
    }

    @Override
    public boolean isEmpty() {
        return this.container.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return this.container.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return this.container.containsValue(value);
    }

    @Override
    public V get(final Object key) {
        return this.container.get(key);
    }

    @Override
    public V put(final K key, final V value) {
        this.inputOrder.put(key, value);
        return this.container.put(key, value);
    }

    @Override
    public V remove(final Object key) {
        this.inputOrder.remove(key);
        return this.container.remove(key);
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        this.inputOrder.putAll(m);
        this.container.putAll(m);
    }

    @Override
    public void clear() {
        this.inputOrder.clear();
        this.container.clear();
    }

    @Override
    public Map.Entry<K, V> lowerEntry(final K key) {
        return this.container.lowerEntry(key);
    }

    @Override
    public K lowerKey(final K key) {
        return this.container.lowerKey(key);
    }

    @Override
    public Map.Entry<K, V> floorEntry(final K key) {
        return this.container.floorEntry(key);
    }

    @Override
    public K floorKey(final K key) {
        return this.container.floorKey(key);
    }

    @Override
    public Map.Entry<K, V> ceilingEntry(final K key) {
        return this.container.ceilingEntry(key);
    }

    @Override
    public K ceilingKey(final K key) {
        return this.container.ceilingKey(key);
    }

    @Override
    public Map.Entry<K, V> higherEntry(final K key) {
        return this.container.higherEntry(key);
    }

    @Override
    public K higherKey(final K key) {
        return this.container.higherKey(key);
    }

    @Override
    public Map.Entry<K, V> firstEntry() {
        return this.container.firstEntry();
    }

    @Override
    public Map.Entry<K, V> lastEntry() {
        return this.container.lastEntry();
    }

    @Override
    public Map.Entry<K, V> pollFirstEntry() {
        final Map.Entry<K, V> entry = this.container.pollFirstEntry();
        this.inputOrder.remove(entry.getKey());
        return entry;
    }

    @Override
    public Map.Entry<K, V> pollLastEntry() {
        final Map.Entry<K, V> entry = this.container.pollLastEntry();
        this.inputOrder.remove(entry.getKey());
        return entry;
    }

    @Override
    public NavigableMap<K, V> descendingMap() {
        return this.container.descendingMap();
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return this.container.navigableKeySet();
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return this.container.descendingKeySet();
    }

    @Override
    public NavigableMap<K, V> subMap(final K fromKey, final boolean fromInclusive, final K toKey, final boolean toInclusive) {
        return this.container.subMap(fromKey, fromInclusive, toKey, toInclusive);
    }

    @Override
    public NavigableMap<K, V> headMap(final K toKey, final boolean inclusive) {
        return this.container.headMap(toKey, inclusive);
    }

    @Override
    public NavigableMap<K, V> tailMap(final K fromKey, final boolean inclusive) {
        return this.container.tailMap(fromKey, inclusive);
    }

    @Override
    public SortedMap<K, V> subMap(final K fromKey, final K toKey) {
        return this.container.subMap(fromKey, toKey);
    }

    @Override
    public SortedMap<K, V> headMap(final K toKey) {
        return this.container.headMap(toKey);
    }

    @Override
    public SortedMap<K, V> tailMap(final K fromKey) {
        return this.container.tailMap(fromKey);
    }

    @Override
    public Map.Entry<K, V> getEldest() {
        return this.inputOrder.getEldest();
    }

    @Override
    public Map.Entry<K, V> removeEldest() {
        final Map.Entry<K, V> eldest = this.inputOrder.removeEldest();
        if (eldest != null) {
            this.container.remove(eldest.getKey());
        }
        return eldest;
    }

    @Override
    public String toString() {
        final StringBuilder buff = new StringBuilder("[").append(this.container.size()).append("]{");
        boolean first = true;
        for (final Map.Entry<K, V> entry : this.container.entrySet()) {
            if (first) {
                first = false;
            } else {
                buff.append(", ");
            }
            buff.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return buff.append("}(").append(this.inputOrder).append(')').toString();
    }

}
