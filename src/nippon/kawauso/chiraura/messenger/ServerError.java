package nippon.kawauso.chiraura.messenger;

import nippon.kawauso.chiraura.lib.connection.PortFunctions;

/**
 * 接続受け付けが開始できない異常。
 * @author chirauraNoSakusha
 */
public final class ServerError implements MessengerReport {

    private final int port;
    private final Exception error;

    ServerError(final int port, final Exception error) {
        if (!PortFunctions.isValid(port)) {
            throw new IllegalArgumentException("Invalid port.");
        } else if (error == null) {
            throw new IllegalArgumentException("Null error.");
        }

        this.port = port;
        this.error = error;
    }

    /**
     * サーバの待機ポート番号を返す。
     * @return サーバの待機ポート番号
     */
    public int getPort() {
        return this.port;
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
                .append('[').append(this.port)
                .append(", ").append(this.error)
                .append(']').toString();
    }

}
