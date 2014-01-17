package nippon.kawauso.chiraura.lib.converter;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * byte[] から16進数文字列への変換・復号。
 * @author chirauraNoSakusha
 */
public final class Hexadecimal {

    /**
     * バイト列を16進数でバイト値を示す文字列に変換する。
     * @param bytes 入力バイト列
     * @return bytesの値を表す文字列。 bytes.lengthが0の場合は""
     */
    public static String toHexadecimal(final byte[] bytes) {
        final char[] buff = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            final int upperValue = (bytes[i] & 0xf0) >> 4;
            final int lowerValue = bytes[i] & 0xf;
            buff[2 * i] = (char) (upperValue < 10 ? '0' + upperValue : 'a' + (upperValue - 10));
            buff[2 * i + 1] = (char) (lowerValue < 10 ? '0' + lowerValue : 'a' + (lowerValue - 10));
        }
        return new String(buff);
    }

    private static int getValue(final char c) throws MyRuleException {
        if ('0' <= c && c <= '9') {
            return c - '0';
        } else if ('A' <= c && c < 'F') {
            return c - 'A' + 10;
        } else if ('a' <= c && c <= 'f') {
            return c - 'a' + 10;
        } else {
            throw new MyRuleException("Invalid character ( " + c + " ).");
        }
    }

    /**
     * 16進数でバイト値を示す文字列からバイト列を復元する。
     * @param string 入力文字列
     * @return 復元したバイト列
     * @throws MyRuleException 16進数でなかった場合と入力長が奇数だった場合
     */
    public static byte[] fromHexadecimal(final String string) throws MyRuleException {
        if (string.length() % 2 != 0) {
            throw new MyRuleException("Odd input length ( " + string.length() + " ).");
        }

        final byte[] buff = new byte[string.length() / 2];
        for (int i = 0; i < buff.length; i++) {
            final int upperValue = getValue(string.charAt(2 * i));
            final int lowerValue = getValue(string.charAt(2 * i + 1));
            buff[i] = (byte) ((upperValue << 4) + lowerValue);
        }
        return buff;
    }

}
