/**
 * 
 */
package nippon.kawauso.chiraura.lib.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * 重複のない乱数列を生成する。
 * @author chirauraNoSakusha
 */
public final class UniqueRandom {

    /**
     * 試行検査方式で計算するかどうかの閾値。
     * 求める値の数 < 値域の幅 * THRESHOLD の場合に試行検査方式を使う。
     */
    private static double THRESHOLD = 0.5;

    private final Random random;

    /**
     * @param random 乱数生成器
     */
    public UniqueRandom(final Random random) {
        this.random = random;
    }

    /**
     * @param seed 乱数種
     */
    public UniqueRandom(final long seed) {
        this(new Random(seed));
    }

    /**
     * 現在時刻を乱数種にして初期化。
     */
    public UniqueRandom() {
        this(new Random());
    }

    /**
     * @param min 生成する乱数の最小値
     * @param upper 生成する乱数の上界
     * @param n 生成する乱数の個数
     * @return 重複の無い乱数の配列
     */
    public int[] getInts(final int min, final int upper, final int n) {
        if (min >= upper) {
            throw new IllegalArgumentException("min (" + min + ") >= upper (" + upper + ").");
        }
        final int width = upper - min;
        if (width < n) {
            throw new IllegalArgumentException("Number of unique numers (" + n + ") >  width of range (" + width + ").");
        }
        if (n < width * THRESHOLD) {
            return getIntByTryAndError(min, upper, n);
        } else {
            return getIntByArraySwap(min, upper, n);
        }
    }

    int collision = 0;

    private int[] getIntByTryAndError(final int min, final int upper, final int n) {
        final int width = upper - min;
        final Set<Integer> set = new HashSet<>();
        while (set.size() < n) {
            if (!set.add(min + this.random.nextInt(width))) {
                this.collision++;
            }
        }

        final int[] values = new int[set.size()];
        int index = 0;
        for (final int value : set) {
            values[index++] = value;
        }
        return values;
    }

    private int[] getIntByArraySwap(final int min, final int upper, final int n) {
        final int width = upper - min;
        final int[] buff = new int[width];
        for (int i = 0; i < width; i++) {
            buff[i] = min + i;
        }
        for (int i = 0; i < n; i++) {
            final int index = i + this.random.nextInt(width - i);
            final int tmp = buff[index];
            buff[index] = buff[i];
            buff[i] = tmp;
        }
        return Arrays.copyOf(buff, n);
    }

}
