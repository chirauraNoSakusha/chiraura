/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.test;

import java.util.Random;

/**
 * @author chirauraNoSakusha
 */
public final class RandomString {

    private final Random random;

    /**
     * @param seed 乱数種
     */
    public RandomString(final long seed) {
        this.random = new Random(seed);
    }

    /**
     * @param length 生成する文字列の長さ
     * @return 生成したランダム文字列
     */
    public String nextString(final int length) {
        return nextString(length, this.random);
    }

    /**
     * @param length 生成する文字列の長さ
     * @param random 乱数生成器
     * @return 生成したランダム文字列
     */
    public static String nextString(final int length, final Random random) {
        final char[] buff = new char[length];
        for (int i = 0; i < length; i++) {
            buff[i] = CharTable.TABLE.charAt(random.nextInt(CharTable.TABLE.length()));
        }
        return new String(buff);
    }

    /**
     * @param length 生成する文字列の長さ
     * @param random 乱数生成器
     * @return 生成したランダム文字列
     */
    public static String nextAsciiString(final int length, final Random random) {
        final char[] buff = new char[length];
        for (int i = 0; i < length; i++) {
            buff[i] = (char) (32 + random.nextInt(127 - 32));
        }
        return new String(buff);
    }

}
