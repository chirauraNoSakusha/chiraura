package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;

/**
 * 在庫確認。
 * @author chirauraNoSakusha
 */
final class CheckStockOperation implements Operation {

    final InetSocketAddress destination;

    CheckStockOperation(final InetSocketAddress destination) {
        if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        }
        this.destination = destination;
    }

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
        } else if (!(obj instanceof CheckStockOperation)) {
            return false;
        }
        /*
         * 1 つの個体に対して 1 つしか実行しない。
         */
        final CheckStockOperation other = (CheckStockOperation) obj;
        return this.destination.equals(other.destination);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.destination)
                .append(']').toString();
    }

}
