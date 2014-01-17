package nippon.kawauso.chiraura.lib.converter;

import java.math.BigInteger;

/**
 * ビット列を逆順に並べる。
 * @author chirauraNoSakusha
 */
public final class BitReversal {

    /**
     * @param input 入力値
     * @return 入力値を表すビット列を逆順に並べたビット列が表す値
     */
    public static byte getByte(final byte input) {
        byte buff = input;
        byte output = 0;
        for (int i = 0; i < Byte.SIZE; i++) {
            output <<= 1;
            output |= buff & 1;
            buff >>= 1;
        }
        return output;
    }

    /**
     * @param input 入力値
     * @return 入力値を表すビット列を逆順に並べたビット列が表す値
     */
    public static short getShort(final short input) {
        short buff = input;
        short output = 0;
        for (int i = 0; i < Short.SIZE; i++) {
            output <<= 1;
            output |= buff & 1;
            buff >>= 1;
        }
        return output;
    }

    /**
     * @param input 入力値
     * @return 入力値を表すビット列を逆順に並べたビット列が表す値
     */
    public static int getInt(final int input) {
        int buff = input;
        int output = 0;
        for (int i = 0; i < Integer.SIZE; i++) {
            output <<= 1;
            output |= buff & 1;
            buff >>= 1;
        }
        return output;
    }

    /**
     * @param input 入力値
     * @return 入力値を表すビット列を逆順に並べたビット列が表す値
     */
    public static long getLong(final long input) {
        long buff = input;
        long output = 0;
        for (int i = 0; i < Long.SIZE; i++) {
            output <<= 1;
            output |= buff & 1;
            buff >>= 1;
        }
        return output;
    }

    /*
     * 計算量がビット数の2乗のため、ビット数が大きいほど getBigInteger(BigInteger,int) より遅くなる。
     * しかも、ビット数が小さくても速いわけではない。
     * シンプルな実装としての遺構。
     */
    static BigInteger getOldBigInteger(final BigInteger input, final int numOfBits) {
        BigInteger output = BigInteger.ZERO;
        for (int i = 0; i < numOfBits; i++) {
            if (input.testBit(i)) {
                output = output.setBit(numOfBits - 1 - i);
            }
        }
        return output;
    }

    /**
     * @param input 任意のBigInteger
     * @param numOfBits 使うビット数
     * @return 入力値を表すビット列を逆順に並べたビット列が表す値
     */
    public static BigInteger getBigInteger(final BigInteger input, final int numOfBits) {
        final byte[] buff = input.toByteArray();
        final byte[] output = new byte[(numOfBits + 7) / 8];

        final int inputHead = Math.max(0, buff.length * 8 - numOfBits); // 前から数えた。
        final int outputHead = output.length * 8 - numOfBits; // 前から数えた。
        final int outputTail = outputHead + Math.min(buff.length * 8, numOfBits); // 前から数えた。
        for (int i = 0, n = Math.min(buff.length * 8, numOfBits); i < n; i++) {
            final int readIndex = inputHead + i; // 前から数えた。
            final int readBytePos = readIndex / 8;
            final int readBitPos = 7 - readIndex % 8;
            final int writeIndex = outputTail - 1 - i; // 前から数えた。
            final int writeBytePos = writeIndex / 8;
            final int writeBitPos = 7 - writeIndex % 8;
            output[writeBytePos] |= ((buff[readBytePos] >> readBitPos) & 1) << writeBitPos;
            // System.out.println("nob:" + numOfBits + " bl:" + (buff.length * 8) + " ol:" + (output.length * 8) + " ih:" + inputHead + " oh:" + outputHead
            // + " ot:" + outputTail + " ri:" + readIndex + " rbp:" + readBytePos + " rbp:" + readBitPos + " wi:" + writeIndex + " wbp:" + writeBytePos
            // + " wbp:" + readBitPos);
        }
        // 先頭を符号で埋める。
        if ((output[0] & (1 << (7 - outputHead))) != 0) {
            for (int i = 0; i < outputHead; i++) {
                output[i / 8] |= 1 << (7 - i % 8);
            }
        }
        // 末尾を符号で埋める
        if ((output[(outputTail - 1) / 8] & (1 << (7 - (outputTail - 1) % 8))) != 0) {
            for (int i = outputTail; i < output.length * 8; i++) {
                output[i / 8] |= 1 << (7 - i % 8);
            }
        }

        return new BigInteger(output);
    }

}
