package nippon.kawauso.chiraura.lib.container;

/**
 * @author chirauraNoSakusha
 * @param <T1> 1つ目の要素のクラス
 * @param <T2> 2つ目の要素のクラス
 * @param <T3> 3つ目の要素のクラス
 */
public final class Triple<T1, T2, T3> {

    private final T1 first;
    private final T2 second;
    private final T3 third;

    /**
     * @param first 1つ目の要素
     * @param second 2つ目の要素
     * @param third 3つ目の要素
     */
    public Triple(final T1 first, final T2 second, final T3 third) {
        this.first = first;
        this.second = second;
        this.third = third;
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

    /**
     * @return 3つ目の要素
     */
    public T3 getThird() {
        return this.third;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.first == null) ? 0 : this.first.hashCode());
        result = prime * result + ((this.second == null) ? 0 : this.second.hashCode());
        result = prime * result + ((this.third == null) ? 0 : this.third.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Triple)) {
            return false;
        }
        final Triple<?, ?, ?> other = (Triple<?, ?, ?>) obj;
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
        if (this.third == null) {
            if (other.third != null) {
                return false;
            }
        } else if (!this.third.equals(other.third)) {
            return false;
        }
        return true;
    }

}
