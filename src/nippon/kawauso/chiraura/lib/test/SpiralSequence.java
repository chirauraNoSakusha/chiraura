/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.test;

/**
 * 数直線で0を中心とした渦巻がたどるような数列。
 *
 * <pre>
 * 0 -1 1 -2 2 -3 3 ... MAX_VALUE MIN_VALUE 0 -1 1 ...
 * </pre>
 * @author chirauraNoSakusha
 */
public final class SpiralSequence {

    private long current;

    /**
     * @param start 開始値
     */
    public SpiralSequence(final long start) {
        this.current = start;
    }

    /**
     * @return 次のlong値
     */
    public long nextLong() {
        final long value = this.current;
        if (this.current == Long.MIN_VALUE) {
            this.current = 0;
        } else {
            if (this.current < 0) {
                this.current = -this.current;
            } else {
                this.current = -(this.current + 1);
            }
        }
        return value;
    }

    /**
     * @return 次のint値
     */
    public int nextInt() {
        final long value = nextLong();
        final long tmp = value & 0x7fffffff;
        return (int) (value >= 0 ? tmp : tmp | 0x80000000);
    }

    /**
     * @return 次のshort値
     */
    public short nextShort() {
        final long value = nextLong();
        final long tmp = value & 0x7fff;
        return (short) (value >= 0 ? tmp : tmp | 0x8000);
    }

    /**
     * @return 次のbyte値
     */
    public short nextByte() {
        final long value = nextLong();
        final long tmp = value & 0x7f;
        return (byte) (value >= 0 ? tmp : tmp | 0x80);
    }

}
