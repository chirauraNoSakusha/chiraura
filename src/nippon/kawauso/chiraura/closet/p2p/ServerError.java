/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.closet.ClosetReport;

/**
 * サーバが起動できない異常。
 * @author chirauraNoSakusha
 */
public final class ServerError implements ClosetReport {

    private final int port;
    private final Exception error;

    ServerError(final nippon.kawauso.chiraura.messenger.ServerError base) {
        if (base == null) {
            throw new IllegalArgumentException("Null base.");
        }

        this.port = base.getPort();
        this.error = base.getError();
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
                .append('[').append(Integer.toString(this.port))
                .append(", ").append(this.error)
                .append(']').toString();
    }

}
