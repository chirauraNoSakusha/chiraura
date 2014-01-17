/**
 * 
 */
package nippon.kawauso.chiraura.lib.container;

/**
 * 簡単な連結リスト用双方向単位容器。
 * @author chirauraNoSakusha
 */
class LinkedCell<T> {

    private final T value;
    private LinkedCell<T> previous;
    private LinkedCell<T> next;

    LinkedCell(final T value) {
        this.value = value;
        this.previous = null;
        this.next = null;
    }

    T get() {
        return this.value;
    }

    LinkedCell<T> getPrevious() {
        return this.previous;
    }

    LinkedCell<T> getNext() {
        return this.next;
    }

    /**
     * 単位容器を後ろに挿入する。
     * @param cell 挿入する単位容器
     */
    void insertNext(final LinkedCell<T> cell) {
        if (this.next != null) {
            this.next.previous = cell;
        }
        cell.next = this.next;
        cell.previous = this;
        this.next = cell;
    }

    /**
     * 単位容器をリストから外す。
     * @return 削除した単位容器。つまり、自身
     */
    LinkedCell<T> remove() {
        if (this.previous != null) {
            this.previous.next = this.next;
        }
        if (this.next != null) {
            this.next.previous = this.previous;
        }
        this.previous = null;
        this.next = null;
        return this;
    }

    /*
     * 以下、簡単な連結リストとしての機能。
     */

    /**
     * 先頭への追加と末尾からの削除のみが可能な連結リストとして使用可能な単位容器を作る。
     * @param <T> 中身のクラス
     * @return 連結リストとして使用可能な単位容器
     */
    static <T> LinkedCell<T> newLinkedList() {
        final LinkedCell<T> list = new LinkedCell<>(null);
        list.clear();
        return list;
    }

    /**
     * 初期化。
     */
    void clear() {
        this.previous = this;
        this.next = this;
    }

    /**
     * 先頭に単位容器を加える。
     * @param cell 加える単位容器
     */
    void addHead(final LinkedCell<T> cell) {
        insertNext(cell);
    }

    /**
     * 末尾の単位容器を返す。
     * @return 末尾の単位容器。
     *         空の場合はnull
     */
    LinkedCell<T> getTail() {
        if (this.next == this) {
            // 空。
            return null;
        } else {
            return this.previous;
        }
    }

    /**
     * 末尾から単位容器を外す。
     * @return 外された単位容器。
     *         空の場合は null
     */
    LinkedCell<T> removeTail() {
        if (this.next == this) {
            // 空。
            return null;
        } else {
            return this.previous.remove();
        }
    }

    @Override
    public String toString() {
        final StringBuilder buff = new StringBuilder();
        if (this.previous != null) {
            buff.append("-(");
        } else {
            buff.append("X(");
        }
        buff.append(this.value);
        if (this.next != null) {
            buff.append(")-");
        } else {
            buff.append(")X");
        }
        return buff.toString();
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LinkedCell)) {
            return false;
        }
        final LinkedCell<?> other = (LinkedCell<?>) obj;
        if (this.value == null) {
            if (other.value != null) {
                return false;
            } else {
                return true;
            }
        }
        return this.value.equals(other.value);
    }

    public static void main(final String[] args) {
        final LinkedCell<Integer> instance = new LinkedCell<>(1243455);
        System.out.println(instance);
        instance.clear();
        System.out.println(instance);
    }

}
