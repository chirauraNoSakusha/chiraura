package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;

/**
 * データ片を取得して復元する。
 * @author chirauraNoSakusha
 */
final class RecoveryOperation implements Operation {

    private final StockEntry destinationStock;

    private final InetSocketAddress destination;

    RecoveryOperation(final StockEntry destinationStock, final InetSocketAddress destination) {
        if (destinationStock == null) {
            throw new IllegalArgumentException("Null destination stock.");
        } else if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        }
        this.destinationStock = destinationStock;
        this.destination = destination;
    }

    StockEntry getDestinationStock() {
        return this.destinationStock;
    }

    InetSocketAddress getDestination() {
        return this.destination;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.destinationStock)
                .append(", ").append(this.destination)
                .append(']').toString();
    }

    @Override
    public int hashCode() {
        return this.destinationStock.getId().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof RecoveryOperation)) {
            return false;
        }
        /*
         * 個体は見ない。
         * 1 つのデータ片に対して 1 つしか実行しない。
         */
        final RecoveryOperation other = (RecoveryOperation) obj;
        return this.destinationStock.getId().equals(other.destinationStock.getId());
    }

}
