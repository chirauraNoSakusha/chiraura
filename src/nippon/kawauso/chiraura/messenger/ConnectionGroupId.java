package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;

/**
 * 識別子。
 * @author chirauraNoSakusha
 */
final class ConnectionGroupId {

    private final InetSocketAddress destination;
    private final int connectionType;

    ConnectionGroupId(final InetSocketAddress destination, final int connectionType) {
        if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        }
        this.destination = destination;
        this.connectionType = connectionType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.destination.hashCode();
        result = prime * result + this.connectionType;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ConnectionGroupId)) {
            return false;
        }
        final ConnectionGroupId other = (ConnectionGroupId) obj;
        return this.destination.equals(other.destination) && this.connectionType == other.connectionType;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.destination)
                .append(", ").append(this.connectionType)
                .append(']').toString();
    }
}