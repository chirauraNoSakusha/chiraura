/**
 * 
 */
package nippon.kawauso.chiraura.lib.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 一番挿入が古い要素の削除が可能なマップ。
 * @author chirauraNoSakusha
 * @param <K> キーのクラス
 * @param <V> 値のクラス
 */
public final class BasicInputOrderedMap<K, V> implements InputOrderedMap<K, V> {

    final Map<K, LinkedCell<Pair<K, V>>> container;
    final LinkedCell<Pair<K, V>> list;

    /**
     * 作成する。
     */
    public BasicInputOrderedMap() {
        this.container = new HashMap<>();
        this.list = LinkedCell.newLinkedList();
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
    public V get(final Object key) {
        final LinkedCell<Pair<K, V>> cell = this.container.get(key);
        if (cell == null) {
            return null;
        } else {
            return cell.get().getSecond();
        }
    }

    @Override
    public V put(final K key, final V value) {
        final LinkedCell<Pair<K, V>> newCell = new LinkedCell<>(new Pair<>(key, value));
        final LinkedCell<Pair<K, V>> oldCell = this.container.put(key, newCell);
        this.list.addHead(newCell);
        if (oldCell == null) {
            return null;
        } else {
            oldCell.remove();
            return oldCell.get().getSecond();
        }
    }

    @Override
    public V remove(final Object key) {
        final LinkedCell<Pair<K, V>> cell = this.container.remove(key);
        if (cell == null) {
            return null;
        } else {
            cell.remove();
            return cell.get().getSecond();
        }
    }

    @Override
    public void clear() {
        this.container.clear();
        this.list.clear();
    }

    @Override
    public Set<K> keySet() {
        return this.container.keySet();
    }

    @Override
    public boolean containsValue(final Object value) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        for (final Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Collection<V> values() {
        final Collection<LinkedCell<Pair<K, V>>> values = this.container.values();
        final List<V> buff = new ArrayList<>(values.size());
        for (final LinkedCell<Pair<K, V>> value : values) {
            buff.add(value.get().getSecond());
        }
        return buff;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        final Set<Map.Entry<K, V>> buff = new HashSet<>();
        for (final Map.Entry<K, LinkedCell<Pair<K, V>>> entry : this.container.entrySet()) {
            buff.add(new BasicEntry<>(entry.getKey(), entry.getValue().get().getSecond()));
        }
        return buff;
    }

    @Override
    public Map.Entry<K, V> removeEldest() {
        final LinkedCell<Pair<K, V>> tail = this.list.removeTail();
        if (tail == null) {
            return null;
        } else {
            this.container.remove(tail.get().getFirst());
            return new BasicEntry<>(tail.get().getFirst(), tail.get().getSecond());
        }
    }

    @Override
    public Map.Entry<K, V> getEldest() {
        final LinkedCell<Pair<K, V>> tail = this.list.getTail();
        if (tail == null) {
            return null;
        } else {
            return new BasicEntry<>(tail.get().getFirst(), tail.get().getSecond());
        }
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder("[")).append(this.container.size()).append("]{");
        boolean first = true;
        for (LinkedCell<Pair<K, V>> cell = this.list.getNext(); cell != this.list; cell = cell.getNext()) {
            if (first) {
                first = false;
            } else {
                buff.append(", ");
            }
            buff.append(cell.get().getFirst()).append('=').append(cell.get().getSecond());
        }
        return buff.append('}').toString();
    }

}
