package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;

import nippon.kawauso.chiraura.closet.ClosetReport;

/**
 * ポートが閉じているかもしれない異常。
 * @author chirauraNoSakusha
 */
public final class ClosePortWarning implements ClosetReport {

    private final int port;
    private final InetSocketAddress destination;

    ClosePortWarning(final nippon.kawauso.chiraura.messenger.ClosePortWarning base) {
        if (base == null) {
            throw new IllegalArgumentException("Null base.");
        }
        this.port = base.getPort();
        this.destination = base.getDestination();
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
    public InetSocketAddress getDestination() {
        return this.destination;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.port)
                .append(']').toString();
    }

}
