/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * long と可変長バイト列との変換。
 * @author chirauraNoSakusha
 */
public final class NumberBytesConversion {

    // インスタンス化防止。
    private NumberBytesConversion() {}

    /**
     * long が64ビットであることに依存している。
     * 
     * <pre>
     * バイト長 先頭ビットパターン 数値部ビット長 補正値                    正値                        負値
     * 1        0                  7              +-0                       0から2^6-1                  -1から-2^6
     * 2        10                 14             +-2^6                     2^6から2^6+2^13-1           -2^6-1から-2^6-2^13
     * 3        110                21             +-(2^6 + 2^13)            (補正値)から(補正値)+2^20-1 -(補正値)-1から-(補正値)-2^20
     * 4        1110               28             +-\sum_{k=1}^{3} 2^{7k-1} (補正値)から(補正値)+2^27-1 -(補正値)-1から-(補正値)-2^27
     * 5        11110              35             +-\sum_{k=1}^{4} 2^{7k-1} (補正値)から(補正値)+2^34-1 -(補正値)-1から-(補正値)-2^34
     * 6        111110             42             +-\sum_{k=1}^{5} 2^{7k-1} (補正値)から(補正値)+2^41-1 -(補正値)-1から-(補正値)-2^41
     * 7        1111110            49             +-\sum_{k=1}^{6} 2^{7k-1} (補正値)から(補正値)+2^48-1 -(補正値)-1から-(補正値)-2^48
     * 8        11111110           56             +-\sum_{k=1}^{7} 2^{7k-1} (補正値)から(補正値)+2^55-1 -(補正値)-1から-(補正値)-2^55
     * 9        11111111           64             +-\sum_{k=1}^{8} 2^{7k-1} (補正値)から2^63-1          -(補正値)-1から-2^63
     * </pre>
     */

    // i バイトのときの補正値を OFFSET[i-1] とする。
    private static final long[] OFFSET_VALUE = new long[9];
    static {
        OFFSET_VALUE[0] = 0;
        for (int i = 1; i < OFFSET_VALUE.length; i++) {
            OFFSET_VALUE[i] = OFFSET_VALUE[i - 1] + (1L << (7 * i - 1));
        }
    }

    // i バイトのときの先頭バイトは (元の値の先頭バイト) | HEAD_PATTERN[i - 1] & AND_MASK[i - 1] となる。
    private static final byte[] HEAD_PATTERN = new byte[9];
    private static final byte[] AND_MASK = new byte[9];
    static {
        for (int i = 0; i < 9; i++) {
            HEAD_PATTERN[i] = (byte) (0x100 - (0x100 >> i));
            AND_MASK[i] = (byte) (0xff - (0x80 >> i));
        }
    }

    /**
     * 変換後のバイト列のサイズ。
     * @param value 入力値
     * @return 必要なバイト数
     */
    public static int byteSize(final long value) {
        if (value >= 0) {
            for (int size = 1; size < OFFSET_VALUE.length; size++) {
                if (value < OFFSET_VALUE[size]) {
                    return size;
                }
            }
            return OFFSET_VALUE.length;
        } else {
            for (int size = 1; size < OFFSET_VALUE.length; size++) {
                if (value >= -OFFSET_VALUE[size]) {
                    return size;
                }
            }
            return OFFSET_VALUE.length;
        }
    }

    /**
     * バイト列に変換する。
     * @param value 入力値
     * @return バイト列
     */
    public static byte[] toBytes(final long value) {
        final int size = byteSize(value);
        final byte[] output = new byte[size];
        long v = (value >= 0 ? value - OFFSET_VALUE[size - 1] : value + OFFSET_VALUE[size - 1]);
        for (int i = size - 1; i >= 0; i--) {
            output[i] = (byte) v;
            v >>= 8;
        }
        // 先頭バイトを整形。
        output[0] = (byte) ((output[0] | HEAD_PATTERN[size - 1]) & AND_MASK[size - 1]);
        return output;
    }

    /**
     * バイト列に変換して書き込む。
     * @param value 入力値
     * @param output 書き込み先
     * @return 書き込みサイズ
     * @throws IOException 書き込みエラー
     */
    public static int toStream(final long value, final OutputStream output) throws IOException {
        final byte[] buff = toBytes(value);
        output.write(buff);
        return buff.length;
    }

    // 先頭バイトからバイト数を得るためのテーブル。
    private static final byte[] HEAD_TO_SIZE = new byte[256];
    static {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            HEAD_TO_SIZE[i & 0xff] = headToSize((byte) i);
        }
    }

    private static byte headToSize(final byte headByte) {
        for (int size = 1; size < HEAD_PATTERN.length; size++) {
            if ((headByte & HEAD_PATTERN[size]) == HEAD_PATTERN[size - 1]) {
                return (byte) size;
            }
        }
        return (byte) HEAD_PATTERN.length;
    }

    private static int decodeSize(final byte headByte) {
        return HEAD_TO_SIZE[headByte & 0xff];
    }

    private static long fromBytes(final byte[] input, final int size) {
        long value = 0;

        // 入力の先頭バイトと出力の上位ビットの処理。
        if (size < 8) {
            // 先頭バイトから値部分を復元。
            final long headValue;
            if ((input[0] & ~AND_MASK[size]) == 0) {
                headValue = (input[0] & ~HEAD_PATTERN[size]) & 0xffL;
            } else {
                headValue = (input[0] | HEAD_PATTERN[size]) & 0xffL;
                // 上位バイトを符号で埋める。
                for (int i = size; i < 8; i++) {
                    value |= 0xffL;
                    value <<= 8;
                }
            }
            value |= headValue;
        } else if (size == 8) {
            // 上位 8 ビットを符号で埋める。
            value = ((input[1] & 0x80) == 0 ? 0L : 0xffL);
        } else {
            // 9 バイトの場合、上位ビットも入力に含まれる。
        }

        // 2 バイト目以降を復元。
        for (int i = 1; i < size; i++) {
            value <<= 8;
            value |= input[i] & 0xffL;
        }

        return (value >= 0 ? value + OFFSET_VALUE[size - 1] : value - OFFSET_VALUE[size - 1]);
    }

    /**
     * バイト列から復元する。
     * @param input 入力
     * @return 復元した値
     * @throws MyRuleException 不正なバイト列だった場合
     */
    public static long fromBytes(final byte[] input) throws MyRuleException {
        if (input.length < 1) {
            throw new MyRuleException("0 length.");
        }
        final int size = decodeSize(input[0]);
        if (input.length < size) {
            throw new MyRuleException("Too short input byte length ( " + input.length + " ) under required size ( " + size + " ).");
        }
        return fromBytes(input, size);
    }

    /**
     * バイト列から復元する。
     * @param input 入力
     * @param maxByteSize 最大読み込みバイト数
     * @param output 復元した値を格納する長さ1以上の配列
     * @return 読み込みサイズ
     * @throws MyRuleException 不正なバイト列だった場合
     * @throws IOException 読み込みエラー
     */
    public static int fromStream(final InputStream input, final int maxByteSize, final long[] output) throws MyRuleException, IOException {
        final byte[] buff = new byte[HEAD_PATTERN.length];
        StreamFunctions.completeRead(input, buff, 0, 1);
        final int size = decodeSize(buff[0]);
        if (maxByteSize < size) {
            throw new MyRuleException("Too short read limit ( " + maxByteSize + " ) under required size ( " + size + " ).");
        }
        StreamFunctions.completeRead(input, buff, 1, size - 1);
        output[0] = fromBytes(buff, size);
        return size;
    }

    /**
     * バイト列から復元する。
     * @param input 入力
     * @return 復元した値
     * @throws MyRuleException 不正なバイト列だった場合
     */
    public static short shortFromBytes(final byte[] input) throws MyRuleException {
        final long value = fromBytes(input);
        if (value < Short.MIN_VALUE || Short.MAX_VALUE < value) {
            throw new MyRuleException("Invalid value ( " + value + " ) for short.");
        }
        return (short) value;
    }

    /**
     * @param input 入力ストリーム
     * @param maxByteSize 最大読み込みバイト数
     * @param output 復元した値を格納する長さ1以上の配列
     * @return 読み込みサイズ
     * @throws MyRuleException 変換規約違反またはintの範囲を超える値が復号された場合
     * @throws IOException 読み込みエラー
     */
    public static int shortFromStream(final InputStream input, final int maxByteSize, final short[] output) throws MyRuleException, IOException {
        final long[] buff = new long[1];
        final int size = fromStream(input, maxByteSize, buff);
        if (buff[0] < Short.MIN_VALUE || Short.MAX_VALUE < buff[0]) {
            throw new MyRuleException("Invalid value ( " + buff[0] + " ) for short.");
        }
        output[0] = (short) buff[0];
        return size;
    }

    /**
     * バイト列から復元する。
     * @param input 入力
     * @return 復元した値
     * @throws MyRuleException 不正なバイト列だった場合
     */
    public static int intFromBytes(final byte[] input) throws MyRuleException {
        final long value = fromBytes(input);
        if (value < Integer.MIN_VALUE || Integer.MAX_VALUE < value) {
            throw new MyRuleException("Invalid value ( " + value + " ) for short.");
        }
        return (int) value;
    }

    /**
     * @param input 入力ストリーム
     * @param maxByteSize 最大読み込みバイト数
     * @param output 復元した値を格納する長さ1以上の配列
     * @return 読み込みサイズ
     * @throws MyRuleException 変換規約違反またはintの範囲を超える値が復号された場合
     * @throws IOException 読み込みエラー
     */
    public static int intFromStream(final InputStream input, final int maxByteSize, final int[] output) throws MyRuleException, IOException {
        final long[] buff = new long[1];
        final int size = fromStream(input, maxByteSize, buff);
        if (buff[0] < Integer.MIN_VALUE || Integer.MAX_VALUE < buff[0]) {
            throw new MyRuleException("Invalid value ( " + buff[0] + " ) for int.");
        }
        output[0] = (int) buff[0];
        return size;
    }

}
