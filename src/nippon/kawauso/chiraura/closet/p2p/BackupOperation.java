/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;

/**
 * @author chirauraNoSakusha
 */
final class BackupOperation implements Operation {

    private final DemandEntry demand;
    private final InetSocketAddress destination;

    BackupOperation(final DemandEntry demand, final InetSocketAddress destination) {
        if (demand == null) {
            throw new IllegalArgumentException("Null demand.");
        } else if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        }
        this.demand = demand;
        this.destination = destination;
    }

    DemandEntry getDemand() {
        return this.demand;
    }

    InetSocketAddress getDestination() {
        return this.destination;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.demand.getId().hashCode();
        result = prime * result + this.destination.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof BackupOperation)) {
            return false;
        }
        final BackupOperation other = (BackupOperation) obj;
        /*
         * 内容は比べない。
         * 1 つの個体、1 つのデータ片に対して 1 つしか実行しない。
         */
        return this.demand.getId().equals(other.demand.getId()) && this.destination.equals(other.destination);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.demand)
                .append(", ").append(this.destination)
                .append(']').toString();
    }

}
