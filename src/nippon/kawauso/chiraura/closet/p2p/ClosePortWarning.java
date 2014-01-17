/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.closet.ClosetReport;

/**
 * ポートが閉じているかもしれない異常。
 * @author chirauraNoSakusha
 */
public final class ClosePortWarning implements ClosetReport {

    private final int port;

    ClosePortWarning(final nippon.kawauso.chiraura.messenger.ClosePortWarning base) {
        if (base == null) {
            throw new IllegalArgumentException("Null base.");
        }
        this.port = base.getPort();
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
