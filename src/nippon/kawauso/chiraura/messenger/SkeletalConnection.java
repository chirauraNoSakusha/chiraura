package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author chirauraNoSakusha
 */
abstract class SkeletalConnection {

    private final int idNumber;
    private final Socket socket;
    private boolean isClosed;

    SkeletalConnection(final int idNumber, final Socket socket) {
        if (socket == null) {
            throw new IllegalArgumentException("Null socket.");
        }

        this.idNumber = idNumber;
        this.socket = socket;
        this.isClosed = false;
    }

    int getIdNumber() {
        return this.idNumber;
    }

    Socket getSocket() {
        return this.socket;
    }

    abstract InetSocketAddress getDestination();

    synchronized boolean isClosed() {
        return this.isClosed;
    }

    synchronized void close() {
        this.isClosed = true;
        try {
            this.socket.close();
        } catch (final IOException ignored) {
        }
    }

    @Override
    public int hashCode() {
        return this.idNumber;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof SkeletalConnection)) {
            return false;
        }
        final SkeletalConnection other = (SkeletalConnection) obj;
        return this.idNumber == other.idNumber;
    }

}
