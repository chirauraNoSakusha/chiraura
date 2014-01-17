/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import nippon.kawauso.chiraura.lib.connection.PortFunctions;

/**
 * ポートが閉じているかもしれない異常。
 * @author chirauraNoSakusha
 */
public final class ClosePortWarning implements MessengerReport {

    private final int port;

    ClosePortWarning(final int port) {
        if (!PortFunctions.isValid(port)) {
            throw new IllegalArgumentException("Invalid port ( " + Integer.toString(port) + " ).");
        }
        this.port = port;
    }

    /**
     * 問題のポートを返す。
     * @return 問題のポート番号
     */
    public int getPort() {
        return this.port;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(Integer.toString(this.port))
                .append(']').toString();
    }

}
