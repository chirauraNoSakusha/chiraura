package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;

import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
public final class CheckOneDemandOperation implements Operation {

    private final Chunk.Id<?> id;
    private final InetSocketAddress destination;

    CheckOneDemandOperation(final Chunk.Id<?> id, final InetSocketAddress destination) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        } else if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        }
        this.id = id;
        this.destination = destination;
    }

    Chunk.Id<?> getId() {
        return this.id;
    }

    InetSocketAddress getDestination() {
        return this.destination;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id)
                .append(", ").append(this.destination)
                .append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + this.destination.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof CheckOneDemandOperation)) {
            return false;
        }
        final CheckOneDemandOperation other = (CheckOneDemandOperation) obj;
        return this.id.equals(other.id) && this.destination.equals(other.destination);
    }

}
