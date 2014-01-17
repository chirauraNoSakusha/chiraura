/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.security.PublicKey;

/**
 * 通信開始の通知。
 * @author chirauraNoSakusha
 */
public final class ConnectReport implements MessengerReport {

    private final PublicKey destinationId;
    private final InetSocketAddress destination;
    private final int connectionType;

    ConnectReport(final PublicKey destinationId, final InetSocketAddress destination, final int connectionType) {
        if (destinationId == null) {
            throw new IllegalArgumentException("Null destination id.");
        } else if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        }
        this.destinationId = destinationId;
        this.destination = destination;
        this.connectionType = connectionType;
    }

    /**
     * 通信相手の識別用鍵を返す。
     * @return 通信相手の識別用鍵
     */
    public PublicKey getDestinationId() {
        return this.destinationId;
    }

    /**
     * 通信相手を返す。
     * @return 通信相手
     */
    public InetSocketAddress getDestination() {
        return this.destination;
    }

    /**
     * 通信路の種別を返す。
     * @return 通信路の種別
     */
    public int getConnectionType() {
        return this.connectionType;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.destination)
                .append(", ").append(Integer.toString(this.connectionType))
                .append(']').toString();
    }

}
