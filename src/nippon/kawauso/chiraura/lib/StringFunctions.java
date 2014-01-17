/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib;

import java.nio.charset.Charset;
import java.util.Arrays;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
public final class StringFunctions {

    // インスタンス化防止。
    private StringFunctions() {}

    /**
     * 文字列を表示したときの幅を計算する。
     * 1バイト文字が幅1、マルチバイト文字が幅2の場合しかうまく動かない タブや制御文字には対応しない。
     * @param str 入力文字列
     * @param charset 文字コード
     * @return 表示幅
     */
    public static int getWidth(final String str, final Charset charset) {
        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            width += (str.charAt(i) < 256 ? 1 : 2);
        }
        return width;
    }

    /**
     * @param str 入力文字列
     * @return 表示幅
     */
    public static int getWidth(final String str) {
        return getWidth(str, Charset.defaultCharset());
    }

    private static boolean allowed(final char c) {
        return ('A' <= c && c <= 'Z')
                || ('a' <= c && c <= 'z')
                || ('0' <= c && c <= '9')
                || c == '-'
                || c == '.'
                || c == '_'
                || c == '~';
    }

    /**
     * URLエンコードされた文字列を元に戻す。
     * @param encoded エンコードされている文字列
     * @param charset 元の文字列の文字コード
     * @return 元の文字列
     * @throws MyRuleException URLエンコードされた文字列として不正な場合
     */
    public static String urlDecode(final String encoded, final Charset charset) throws MyRuleException {
        final byte[] buff = new byte[encoded.length()]; // バイト長は文字列長を超えない。
        int buffIndex = 0;
        for (int i = 0; i < encoded.length(); i++) {
            if (encoded.charAt(i) == '%') {
                if (i + 2 >= encoded.length()) {
                    throw new MyRuleException("More characters are required after " + encoded.substring(i) + ".");
                }
                try {
                    buff[buffIndex++] = (byte) Integer.parseInt(encoded.substring(i + 1, i + 3), 16);
                } catch (final NumberFormatException e) {
                    throw new MyRuleException(e);
                }
                i += 2;
            } else if (allowed(encoded.charAt(i))) {
                buff[buffIndex++] = (byte) encoded.charAt(i);
            } else {
                throw new MyRuleException("Invalid character ( " + encoded.charAt(i) + " ) at " + i + ".");
            }
        }

        return new String(Arrays.copyOf(buff, buffIndex), charset);
    }

    private static String urlEncode(final char c, final Charset charset) {
        if (allowed(c)) {
            return Character.toString(c);
        } else {
            final byte[] bytes = Character.toString(c).getBytes(charset);
            final char[] chars = new char[3 * bytes.length];
            int length = 0;
            for (int i = 0; i < bytes.length; i++) {
                if (i != 0 && allowed((char) (bytes[i] & 0xff))) {
                    chars[length++] = (char) (bytes[i] & 0xff);
                } else {
                    final int upper = (bytes[i] >> 4) & 0xf;
                    final int lower = bytes[i] & 0xf;
                    chars[length++] = '%';
                    chars[length++] = (char) (upper < 10 ? '0' + upper : 'A' + upper - 10);
                    chars[length++] = (char) (lower < 10 ? '0' + lower : 'A' + lower - 10);
                }
            }
            return new String(chars, 0, length);
        }
    }

    /**
     * URLエンコードする。
     * @param string 元の文字列
     * @param charset 文字コード
     * @return URLエンコードされた文字列
     */
    public static String urlEncode(final String string, final Charset charset) {
        final StringBuilder buff = new StringBuilder();
        for (final char c : string.toCharArray()) {
            buff.append(urlEncode(c, charset));
        }
        return buff.toString();
    }

}
