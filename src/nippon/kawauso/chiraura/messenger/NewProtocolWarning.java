/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

/**
 * ポートが閉じているかもしれない異常。
 * @author chirauraNoSakusha
 */
public final class NewProtocolWarning implements MessengerReport {

    private final long version;
    private final long newVersion;

    NewProtocolWarning(final long version, final long newVersion) {
        if (newVersion <= version) {
            throw new IllegalArgumentException("New version ( " + Long.toString(newVersion) + " ) smaller current version ( " + Long.toString(version) + " ).");
        }
        this.version = version;
        this.newVersion = newVersion;
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
