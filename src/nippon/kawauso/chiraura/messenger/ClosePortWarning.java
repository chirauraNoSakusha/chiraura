/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetAddress;

import nippon.kawauso.chiraura.lib.connection.PortFunctions;

/**
 * ポートが閉じているかもしれない異常。
 * @author chirauraNoSakusha
 */
public final class ClosePortWarning implements MessengerReport {

    private final int port;
    private final InetAddress destination;

    ClosePortWarning(final int port, final InetAddress destination) {
        if (!PortFunctions.isValid(port)) {
            throw new IllegalArgumentException("Invalid port ( " + port + " ).");
        } else if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        }
        this.port = port;
        this.destination = destination;
    }

    /**
     * 問題のポートを返す。
     * @return 問題のポート番号
     */
    public int getPort() {
        return this.port;
    }

    /**
     * 通信先を返す。
     * @return 通信先
     */
    public InetAddress getDestination() {
        return this.destination;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.port)
                .append(']').toString();
    }

}
