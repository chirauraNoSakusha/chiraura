/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.container;

/**
 * @param <T1> 1つ目の要素のクラス
 * @param <T2> 1つ目の要素のクラス
 * @author chirauraNoSakusha
 */
public final class Pair<T1, T2> {

    private final T1 first;
    private final T2 second;

    /**
     * @param first 1つ目の要素
     * @param second 2つ目の要素
     */
    public Pair(final T1 first, final T2 second) {
        this.first = first;
        this.second = second;
    }

    /**
     * @return 1つ目の要素
     */
    public T1 getFirst() {
        return this.first;
    }

    /**
     * @return 2つ目の要素
     */
    public T2 getSecond() {
        return this.second;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.first == null) ? 0 : this.first.hashCode());
        result = prime * result + ((this.second == null) ? 0 : this.second.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Pair)) {
            return false;
        }
        final Pair<?, ?> other = (Pair<?, ?>) obj;
        if (this.first == null) {
            if (other.first != null) {
                return false;
            }
        } else if (!this.first.equals(other.first)) {
            return false;
        }
        if (this.second == null) {
            if (other.second != null) {
                return false;
            }
        } else if (!this.second.equals(other.second)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return (new StringBuilder("(")).append(this.first)
                .append(", ").append(this.second)
                .append(')').toString();
    }

}
