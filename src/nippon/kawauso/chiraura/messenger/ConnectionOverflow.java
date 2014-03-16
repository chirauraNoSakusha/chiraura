package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;

/**
 * 接続数過多。
 * @author chirauraNoSakusha
 */
public final class ConnectionOverflow implements MessengerReport {

    private final InetSocketAddress destination;

    ConnectionOverflow(final InetSocketAddress destination) {
        if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        }

        this.destination = destination;
    }

    /**
     * 異常が起きた通信相手を返す。
     * @return 異常が起きた通信相手
     */
    public InetSocketAddress getDestination() {
        return this.destination;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.destination)
                .append(']').toString();
    }

}
