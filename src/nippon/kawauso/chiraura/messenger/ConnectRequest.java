/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;

/**
 * 自分から他の個体への接続要請。
 * @author chirauraNoSakusha
 */
final class ConnectRequest {

    private final InetSocketAddress destination;
    private final int connectionType;

    ConnectRequest(final InetSocketAddress destination, final int connectionType) {
        if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        }

        this.destination = destination;
        this.connectionType = connectionType;
    }

    InetSocketAddress getDestination() {
        return this.destination;
    }

    int getConnectionType() {
        return this.connectionType;
    }

}
