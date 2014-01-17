/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.closet.ClosetReport;

/**
 * 動作規約が合致しない異常。
 * @author chirauraNoSakusha
 */
public final class NewProtocolWarning implements ClosetReport {

    private final long version;
    private final long newVersion;

    NewProtocolWarning(final nippon.kawauso.chiraura.messenger.NewProtocolWarning base) {
        if (base == null) {
            throw new IllegalArgumentException("Null base.");
        }
        this.version = base.getVersion();
        this.newVersion = base.getNewVersion();
    }

    /**
     * 実行中の版を返す。
     * @return 実行中の版
     */
    public long getVersion() {
        return this.version;
    }

    /**
     * 新しい版を返す。
     * @return 新しい版
     */
    public long getNewVersion() {
        return this.newVersion;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.version)
                .append(", ").append(this.newVersion)
                .append(']').toString();
    }

}
