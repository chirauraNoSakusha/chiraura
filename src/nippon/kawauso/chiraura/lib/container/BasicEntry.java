package nippon.kawauso.chiraura.lib.container;

import java.util.Map;

/**
 * Map.Entry の実装。
 * @author chirauraNoSakusha
 * @param <K> キーのクラス
 * @param <V> 値のクラス
 */
final class BasicEntry<K, V> implements Map.Entry<K, V> {
    private final K key;
    private final V value;

    /**
     * 作成する。
     * @param key キー
     * @param value 値
     */
    BasicEntry(final K key, final V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public K getKey() {
        return this.key;
    }

    @Override
    public V getValue() {
        return this.value;
    }

    @Override
    public V setValue(final V newValue) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.key.toString()))
                .append('=').append(this.value)
                .toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.key == null) ? 0 : this.key.hashCode());
        result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Map.Entry)) {
            return false;
        }
        final BasicEntry<?, ?> other = (BasicEntry<?, ?>) obj;
        if (this.key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!this.key.equals(other.key)) {
            return false;
        }
        if (this.value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!this.value.equals(other.value)) {
            return false;
        }
        return true;
    }

}
