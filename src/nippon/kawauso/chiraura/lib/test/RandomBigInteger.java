package nippon.kawauso.chiraura.lib.test;

import java.math.BigInteger;
import java.util.Random;

/**
 * @author chirauraNoSakusha
 */
public final class RandomBigInteger {

    // インスタンス化防止。
    private RandomBigInteger() {}

    /**
     * 正の乱数を返す。
     * @param random 乱数
     * @param numberOfBits ビット数
     * @return 正の乱数
     */
    public static BigInteger positive(final Random random, final int numberOfBits) {
        final int size = (numberOfBits + Byte.SIZE) / Byte.SIZE; // 先頭を 0 にするため、最低 1 ビットの余白を入れる。
        final byte[] buff = new byte[size];
        random.nextBytes(buff);
        final int blankPos = numberOfBits % Byte.SIZE;
        if (blankPos == 0) {
            buff[0] = 0;
        } else {
            buff[0] &= (1 << blankPos) - 1;
        }
        return new BigInteger(buff);
    }

}
