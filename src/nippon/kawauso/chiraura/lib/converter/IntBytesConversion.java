package nippon.kawauso.chiraura.lib.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 整数とバイト列の変換。
 * @author chirauraNoSakusha
 */
public final class IntBytesConversion {

    // インスタンス化防止。
    private IntBytesConversion() {}

    /**
     * バイトサイズ。
     */
    public static final int BYTE_SIZE = Integer.SIZE / Byte.SIZE;

    /**
     * 書き込みサイズを返す。
     * @param value 入力値
     * @return 書き込みサイズ
     */
    public static int byteSize(final int value) {
        return BYTE_SIZE;
    }

    private static byte[] toBytes(final int value) {
        int v = value;
        final byte[] output = new byte[BYTE_SIZE];
        for (int i = BYTE_SIZE - 1; i >= 0; i--) {
            output[i] = (byte) v;
            v >>= Byte.SIZE;
        }
        return output;
    }

    /**
     * バイト列に変換して書き込む。
     * @param value 入力値
     * @param output 書き込み先
     * @return 書き込みサイズ。
     * @throws IOException 書き込みエラー
     */
    public static int toStream(final int value, final OutputStream output) throws IOException {
        output.write(toBytes(value));
        return BYTE_SIZE;
    }

    private static int fromBytes(final byte[] input) {
        int v = 0;
        for (int i = 0; i < BYTE_SIZE; i++) {
            v <<= Byte.SIZE;
            v |= input[i] & ((1 << Byte.SIZE) - 1);
        }
        return v;
    }

    /**
     * バイト列から復元する。
     * @param input 入力
     * @param output 復元した値を格納する長さ1以上の配列
     * @return 復元した値
     * @throws MyRuleException 不正なバイト列だった場合
     * @throws IOException 読み込みエラー
     */
    public static int fromStream(final InputStream input, final int[] output) throws MyRuleException, IOException {
        output[0] = fromBytes(StreamFunctions.completeRead(input, BYTE_SIZE));
        return BYTE_SIZE;
    }

}
