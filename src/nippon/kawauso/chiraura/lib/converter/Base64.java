package nippon.kawauso.chiraura.lib.converter;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * バイト列からBase64形式の文字列への変換・復号。
 * Base64形式の文字列の長さは、バイト列の全ビットを表すに足る最短の長さとする。
 * @author chirauraNoSakusha
 */
public final class Base64 {

    // インスタンス化防止。
    private Base64() {}

    private static final char[] TABLE = new char[62];
    static {
        int index = 0;
        for (char c = 'A'; c <= 'Z'; c++) {
            TABLE[index] = c;
            index++;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            TABLE[index] = c;
            index++;
        }
        for (char c = '0'; c <= '9'; c++) {
            TABLE[index] = c;
            index++;
        }
        assert index == TABLE.length;
    }

    private static final char DEFAULT_63 = '+';
    private static final char DEFAULT_64 = '/';

    private static char getChar(final int value, final char c63, final char c64) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative value ( " + value + " ).");
        } else if (value < 62) {
            return TABLE[value];
        } else if (value == 62) {
            return c63;
        } else if (value == 63) {
            return c64;
        } else {
            throw new IllegalArgumentException("Too large value ( " + value + " ) over limit ( 63 ).");
        }
    }

    private static int getValue(final char c, final char c63, final char c64) throws MyRuleException {
        if ('A' <= c && c <= 'Z') {
            return c - 'A';
        } else if ('a' <= c && c <= 'z') {
            return c - 'a' + ('Z' - 'A' + 1);
        } else if ('0' <= c && c <= '9') {
            return c - '0' + ('z' - 'a' + 1) + ('Z' - 'A' + 1);
        } else if (c == c63) {
            return 62;
        } else if (c == c64) {
            return 63;
        } else {
            throw new MyRuleException("Invalid character ( " + c + " ) not in [A-Za-z0-9" + c63 + "" + c64 + "].");
        }
    }

    /**
     * @param input 任意のバイト列
     * @return 入力バイト列を表すBase64形式の文字列
     */
    public static String toBase64(final byte[] input) {
        return toBase64(input, DEFAULT_63, DEFAULT_64);
    }

    /**
     * @param input 任意のバイト列
     * @param c63 63番目の文字
     * @param c64 64番目の文字
     * @return 入力バイト列を表すBase64形式の文字列
     */
    public static String toBase64(final byte[] input, final char c63, final char c64) {
        /*
         * |-+-+-+-+-+-+-+-|-+-+-+-+-+-+-+-|-+-+-+-+-+-+-+-|-... 入力バイト列のビットの区切り
         * |-+-+-+-+-+-|-+-+-+-+-+-|-+-+-+-+-+-|-+-+-+-+-+-|-... 出力文字列の表すビットの区切り
         */
        final char[] output = new char[(input.length * 4 + 2) / 3];
        for (int writeIndex = 0; writeIndex < output.length; writeIndex++) {
            final int readIndex = (writeIndex * 6) / 8;
            final int value;
            if (writeIndex % 4 == 0) {
                value = (input[readIndex] & 0xff) >> 2;
            } else if (writeIndex % 4 == 1) {
                if (readIndex < input.length - 1) {
                    value = ((input[readIndex] & 0x3) << 4) + ((input[readIndex + 1] & 0xf0) >> 4);
                } else {
                    value = ((input[readIndex] & 0x3) << 4);
                }
            } else if (writeIndex % 4 == 2) {
                if (readIndex < input.length - 1) {
                    value = ((input[readIndex] & 0xf) << 2) + ((input[readIndex + 1] & 0xc0) >> 6);
                } else {
                    value = ((input[readIndex] & 0xf) << 2);
                }
            } else {
                value = input[readIndex] & 0x3f;
            }
            output[writeIndex] = getChar(value, c63, c64);
        }
        return new String(output);
    }

    /**
     * @param input Base64形式の文字列
     * @return 復号したバイト列
     * @throws MyRuleException 入力文字列の規約違反
     */
    public static byte[] fromBase64(final String input) throws MyRuleException {
        return fromBase64(input, DEFAULT_63, DEFAULT_64);
    }

    /**
     * @param input Base64形式の文字列
     * @param c63 63番目の文字
     * @param c64 64番目の文字
     * @return 復号したバイト列
     * @throws MyRuleException 入力文字列の規約違反
     */
    public static byte[] fromBase64(final String input, final char c63, final char c64) throws MyRuleException {
        /*
         * |-+-+-+-+-+-|-+-+-+-+-+-|-+-+-+-+-+-|-+-+-+-+-+-|-... 入力文字列の表すビットの区切り
         * |-+-+-+-+-+-+-+-|-+-+-+-+-+-+-+-|-+-+-+-+-+-+-+-|-... 出力バイト列のビットの区切り
         */
        if (input.length() % 4 == 1) {
            /*
             * 出力バイト列の長さが 0 (mod 3) のときは入力文字列の長さは 0 (mod 4)。
             * 出力バイト列の長さが 1 (mod 3) のときは入力文字列の長さは 2 (mod 4)。
             * 出力バイト列の長さが 2 (mod 3) のときは入力文字列の長さは 3 (mod 4)。
             */
            throw new MyRuleException("Invalid input length ( " + input.length() + " ).");
        } else if (input.length() % 4 == 2) {
            final byte remain = (byte) (getValue(input.charAt(input.length() - 1), c63, c64) & 0xf);
            if (remain != 0) {
                throw new MyRuleException("Cannot use the lower bits of last character ( " + input.charAt(input.length() - 1) + " ).");
            }
        } else if (input.length() % 4 == 3) {
            final byte remain = (byte) (getValue(input.charAt(input.length() - 1), c63, c64) & 0x3);
            if (remain != 0) {
                throw new MyRuleException("Cannot use the lower bits of last character ( " + input.charAt(input.length() - 1) + " ).");
            }
        }

        final byte[] output = new byte[input.length() * 3 / 4];
        for (int writeIndex = 0; writeIndex < output.length; writeIndex++) {
            final int readIndex = (writeIndex * 8) / 6;
            if (writeIndex % 3 == 0) {
                output[writeIndex] = (byte) ((getValue(input.charAt(readIndex), c63, c64) << 2) + (getValue(input.charAt(readIndex + 1), c63, c64) >> 4));
            } else if (writeIndex % 3 == 1) {
                output[writeIndex] = (byte) ((getValue(input.charAt(readIndex), c63, c64) << 4) + (getValue(input.charAt(readIndex + 1), c63, c64) >> 2));
            } else {
                output[writeIndex] = (byte) ((getValue(input.charAt(readIndex), c63, c64) << 6) + getValue(input.charAt(readIndex + 1), c63, c64));
            }
        }
        return output;
    }

}
