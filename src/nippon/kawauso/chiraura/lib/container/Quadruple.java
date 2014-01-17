package nippon.kawauso.chiraura.lib.container;

/**
 * @author chirauraNoSakusha
 * @param <T1> 1つ目の要素のクラス
 * @param <T2> 2つ目の要素のクラス
 * @param <T3> 3つ目の要素のクラス
 * @param <T4> 4つ目の要素のクラス
 */
public final class Quadruple<T1, T2, T3, T4> {

    private final T1 first;
    private final T2 second;
    private final T3 third;
    private final T4 fourth;

    /**
     * @param first 1つ目の要素
     * @param second 2つ目の要素
     * @param third 3つ目の要素
     * @param fourth 4つ目の要素
     */
    public Quadruple(final T1 first, final T2 second, final T3 third, final T4 fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
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

    /**
     * @return 4つ目の要素
     */
    public T4 getFourth() {
        return this.fourth;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.first == null) ? 0 : this.first.hashCode());
        result = prime * result + ((this.fourth == null) ? 0 : this.fourth.hashCode());
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
        if (!(obj instanceof Quadruple)) {
            return false;
        }
        final Quadruple<?, ?, ?, ?> other = (Quadruple<?, ?, ?, ?>) obj;
        if (this.first == null) {
            if (other.first != null) {
                return false;
            }
        } else if (!this.first.equals(other.first)) {
            return false;
        }
        if (this.fourth == null) {
            if (other.fourth != null) {
                return false;
            }
        } else if (!this.fourth.equals(other.fourth)) {
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
