package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.closet.ClosetReport;

/**
 * 動作規約が合致しない異常。
 * @author chirauraNoSakusha
 */
public final class NewProtocolWarning implements ClosetReport {

    private final long majorGap;
    private final long minorGap;

    NewProtocolWarning(final long majorGap, final long minorGap) {
        if (majorGap < 0) {
            throw new IllegalArgumentException("Negative major gap ( " + majorGap + " ).");
        } else if (minorGap < 0) {
            throw new IllegalArgumentException("Negative minor gap ( " + minorGap + " ).");
        } else if (majorGap == 0 && minorGap == 0) {
            throw new IllegalArgumentException("No gap.");
        }
        this.majorGap = majorGap;
        this.minorGap = minorGap;
    }

    /**
     * メジャーバージョンの差を返す。
     * @return メジャーバージョンの差
     */
    public long getMajorGap() {
        return this.majorGap;
    }

    /**
     * マイナーバージョンの差を返す。
     * @return マイナーバージョンの差
     */
    public long getMinorGap() {
        return this.minorGap;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.majorGap)
                .append(", ").append(this.minorGap)
                .append(']').toString();
    }

}
