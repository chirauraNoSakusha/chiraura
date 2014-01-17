/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;

/**
 * @author chirauraNoSakusha
 */
final class FirstAccessOperation implements Operation {

    private final InetSocketAddress destination;

    FirstAccessOperation(final InetSocketAddress destination) {
        if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        }
        this.destination = destination;
    }

    /**
     * 目標個体を返す。
     * @return 目標個体
     */
    InetSocketAddress getDestination() {
        return this.destination;
    }

    @Override
    public int hashCode() {
        return this.destination.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof FirstAccessOperation)) {
            return false;
        }
        /*
         * 1 つの個体に対して 1つしか実行しない。
         */
        final FirstAccessOperation other = (FirstAccessOperation) obj;
        return this.destination.equals(other.destination);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.destination)
                .append(']').toString();
    }

}
