/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;

/**
 * 自分から他の個体へ接続している最中の異常。
 * @author chirauraNoSakusha
 */
public final class ContactError implements MessengerReport {

    private final InetSocketAddress destination;
    private final Exception error;

    ContactError(final InetSocketAddress destination, final Exception error) {
        if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        }
        this.destination = destination;
        this.error = error;
    }

    /**
     * 異常が起きた通信相手を返す。
     * @return 異常が起きた通信相手
     */
    public InetSocketAddress getDestination() {
        return this.destination;
    }

    /**
     * 発生した異常を返す。
     * @return 発生した異常
     */
    public Exception getError() {
        return this.error;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.destination)
                .append(", ").append(this.error)
                .append(']').toString();
    }

}
