/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.math;

import java.math.BigInteger;

/**
 * @author chirauraNoSakusha
 */
public final class MathFunctions {

    // インスタンス化防止。
    private MathFunctions() {}

    /**
     * 立っている最上位ビットの位置を求める。
     * @param value 入力値k
     * @param highestBit 最上位のビット位置
     * @return value.testBit(n) == trueとなるhigestBit以下で最大のn。
     *         valueが0のときは-1。
     */
    public static int getHighestSetBit(final BigInteger value, final int highestBit) {
        for (int i = highestBit; i >= 0; i--) {
            if (value.testBit(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 最大値を求める。
     * @param firstValue 最初の候補
     * @param values 候補
     * @return 最大値
     */
    public static int max(final int firstValue, final int... values) {
        int max = firstValue;
        for (final int value : values) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    /**
     * 2 を底とする対数を返す。
     * @param value 入力値
     * @return 入力値の 2 を底とする対数
     */
    public static double log2(final double value) {
        return Math.log(value) / Math.log(2.0);
    }

}
