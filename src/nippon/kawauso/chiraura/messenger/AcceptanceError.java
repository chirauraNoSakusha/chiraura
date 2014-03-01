/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;

/**
 * 接続先の特定中の異常。
 * @author chirauraNoSakusha
 */
public final class AcceptanceError implements MessengerReport {

    // 受け付けた接続では、相手の受け付けポート番号は不明なためポートは一時ポート。
    private final InetSocketAddress destination;
    private final Exception error;

    AcceptanceError(final InetSocketAddress destination, final Exception error) {
        if (destination == null) {
            throw new IllegalArgumentException("Null destination IP address.");
        } else if (error == null) {
            throw new IllegalArgumentException("Null error.");
        }

        this.destination = destination;
        this.error = error;
    }

    /**
     * 異常が起きた通信相手を返す。
     * 相手の受け付けポート番号は不明なためポートは一時ポート。
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
