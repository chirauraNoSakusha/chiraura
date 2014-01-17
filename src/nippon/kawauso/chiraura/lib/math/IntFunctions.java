/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.math;

/**
 * @author chirauraNoSakusha
 */
public final class IntFunctions {

    // インスタンス化防止。
    private IntFunctions() {}

    /**
     * @param firstValue 最初の値
     * @param values 2番目以降の値
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

}
