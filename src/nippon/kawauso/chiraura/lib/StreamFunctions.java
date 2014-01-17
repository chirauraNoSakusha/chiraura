/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib;

import java.io.IOException;
import java.io.InputStream;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
public final class StreamFunctions {

    // インスタンス化防止。
    private StreamFunctions() {}

    /*
     * TODO 以下の completeXxx において入力が足りなかったときは MyRuleException を返すようにしているが,
     * 読み込み長は呼び出し側が与えるため、規約違反(MyRuleException)より相応わしい例外がありそう。
     */

    /**
     * サイズぴったりまで読み込む。
     * @param input 読み込み元
     * @param length 読み込みサイズ
     * @return 読み込み結果
     * @throws MyRuleException 読み込める量が足りなかった場合
     * @throws IOException 読み込みエラー
     */
    public static byte[] completeRead(final InputStream input, final int length) throws MyRuleException, IOException {
        final byte[] buff = new byte[length];
        completeRead(input, buff, 0, length);
        return buff;
    }

    /**
     * サイズぴったりまで読み込む。
     * @param input 読み込み元
     * @param out 読み込み結果の書き込み先
     * @param offset 書き込み開始位置
     * @param length 読み込むサイズ
     * @return 読み込んだサイズ。length と等しい
     * @throws MyRuleException 読み込める量が足りなかった場合
     * @throws IOException 読み込みエラー
     */
    public static int completeRead(final InputStream input, final byte[] out, final int offset, final int length) throws MyRuleException, IOException {
        for (int i = 0, l; i < length; i += l) {
            l = input.read(out, offset + i, length - i);
            if (l < 0) {
                throw new MyRuleException("Read only " + i + " bytes smaller than required size ( " + length + " ).");
            }
        }
        return length;
    }

    /**
     * サイズ分飛ばす。
     * @param input 読み込み元
     * @param length 飛ばすバイト数
     * @return 飛ばしたバイト数。length と等しい
     * @throws MyRuleException 指定されたバイト数分飛ばす前に入力が終了した場合
     * @throws IOException 読み込みエラー
     */
    public static int completeSkip(final InputStream input, final int length) throws MyRuleException, IOException {
        for (int i = 0, l; i < length; i += l) {
            if (isEof(input)) {
                throw new MyRuleException("Skip only " + i + " bytes smaller than required size ( " + length + " ).");
            }
            l = (int) input.skip(length - i);
        }
        return length;
    }

    /**
     * ストリームの終わりに達したかどうか調べる。
     * @param input mark()をサポートする読み込み元
     * @return 終わりに達していたらtrue、そうでなければfalse
     * @throws IOException 読み込みエラー
     */
    public static boolean isEof(final InputStream input) throws IOException {
        if (input.available() > 0) {
            return false;
        }
        input.mark(1);
        final int length = input.read(new byte[1], 0, 1);
        input.reset();
        if (length < 0) {
            return true;
        }
        return false;
    }

}
