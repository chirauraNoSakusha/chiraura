/**
 * 
 */
package nippon.kawauso.chiraura.lib.test;

import java.util.Random;

/**
 * 偏りのある乱数の生成器。
 * @author chirauraNoSakusha
 */
public final class BiasedRandom {

    private final Random random;

    /**
     * 乱数生成器を指定して作成。
     * @param random 一様乱数の生成器
     */
    public BiasedRandom(final Random random) {
        this.random = random;
    }

    /**
     * 乱数を得る。
     * @return [0.0, 1.0) の乱数
     */
    public double next() {
        return calculate(this.random.nextDouble());
    }

    /**
     * 偏りのある乱数を計算する。
     * @param uniform 一様乱数
     * @return 乱数
     */
    private static double calculate(final double uniform) {
        /*
         * 羃分布の一種である (1 / x) をx軸負方向に offset だけずらした分布の [0.0, right) 分を [0.0, 1.0) に正規化した分布。
         */
        final double offset = 0.05;
        final double right = 1.0 + offset;
        return offset * (Math.pow(Math.E, uniform * Math.log(right / offset + 1.0)) - 1.0) / right;
    }

}
