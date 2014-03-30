package nippon.kawauso.chiraura.lib.converter;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * バイト列から Base32 形式の文字列への変換・復号。
 * Base32 形式の文字列の長さは、バイト列の全ビットを表すに足る最短の長さとする。
 * @author chirauraNoSakusha
 */
public final class Base32 {

    // インスタンス化防止。
    private Base32() {}

    private static final char[] TABLE = new char[32];
    static {
        int index = 0;
        for (char c = 'A'; c <= 'Z'; c++) {
            TABLE[index] = c;
            index++;
        }
        for (char c = '2'; c <= '7'; c++) {
            TABLE[index] = c;
            index++;
        }
        assert index == TABLE.length;
    }

    private static char getChar(final int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative value ( " + value + " ).");
        } else if (value < TABLE.length) {
            return TABLE[value];
        } else {
            throw new IllegalArgumentException("Too large value ( " + value + " ) over limit ( " + (TABLE.length - 1) + " ).");
        }
    }

    private static int getValue(final char c) throws MyRuleException {
        if ('A' <= c && c <= 'Z') {
            return c - 'A';
        } else if ('2' <= c && c <= '7') {
            return c - '2' + ('Z' - 'A' + 1);
        } else {
            throw new MyRuleException("Invalid character ( " + c + " ) not in [A-Z2-7].");
        }
    }

    /**
     * @param input 任意のバイト列
     * @return 入力バイト列を表すBase32形式の文字列
     */
    public static String toBase32(final byte[] input) {
        /**
         * <pre>
         * |-+-+-+-+-+-+-+-|-+-+-+-+-+-+-+-|-+-+-+-+-+-+-+-|-+-+-+-+-+-+-+-|-+-+-+-+-+-+-+-|-... 入力バイト列のビットの区切り
         * |-+-+-+-+-|-+-+-+-+-|-+-+-+-+-|-+-+-+-+-|-+-+-+-+-|-+-+-+-+-|-+-+-+-+-|-+-+-+-+-|-... 出力文字列の表すビットの区切り
         * 0         1         2         3         4         5         6         7
         * </pre>
         */
        final char[] output = new char[(input.length * 8 + 4) / 5];
        for (int writeIndex = 0; writeIndex < output.length; writeIndex++) {
            final int readIndex = (writeIndex * 5) / 8;
            final int value;
            switch (writeIndex % 8) {
            case 0:
                value = (input[readIndex] & 0xff) >> 3;
                break;
            case 1:
                if (readIndex < input.length - 1) {
                    value = ((input[readIndex] & 0x7) << 2) + ((input[readIndex + 1] & 0xf0) >> 6);
                } else {
                    value = (input[readIndex] & 0x7) << 2;
                }
                break;
            case 2:
                value = (input[readIndex] & 0x3f) >> 1;
                break;
            case 3:
                if (readIndex < input.length - 1) {
                    value = ((input[readIndex] & 0x1) << 4) + ((input[readIndex + 1] & 0xf0) >> 4);
                } else {
                    value = (input[readIndex] & 0x1) << 4;
                }
                break;
            case 4:
                if (readIndex < input.length - 1) {
                    value = ((input[readIndex] & 0xf) << 1) + ((input[readIndex + 1] & 0xf0) >> 7);
                } else {
                    value = (input[readIndex] & 0xf) << 1;
                }
                break;
            case 5:
                value = (input[readIndex] & 0x7f) >> 2;
                break;
            case 6:
                if (readIndex < input.length - 1) {
                    value = ((input[readIndex] & 0x3) << 3) + ((input[readIndex + 1] & 0xf0) >> 5);
                } else {
                    value = (input[readIndex] & 0x3) << 3;
                }
                break;
            default:
                value = input[readIndex] & 0x1f;
                break;
            }
            output[writeIndex] = getChar(value);
        }
        return new String(output);
    }

    /**
     * @param input Base32形式の文字列
     * @return 復号したバイト列
     * @throws MyRuleException 入力文字列の規約違反
     */
    public static byte[] fromBase32(final String input) throws MyRuleException {
        /**
         * <pre>
         * 0         1         2         3         4         5         6         7
         * |-+-+-+-+-|-+-+-+-+-|-+-+-+-+-|-+-+-+-+-|-+-+-+-+-|-+-+-+-+-|-+-+-+-+-|-+-+-+-+-|-... 入力文字列の表すビットの区切り
         * |-+-+-+-+-+-+-+-|-+-+-+-+-+-+-+-|-+-+-+-+-+-+-+-|-+-+-+-+-+-+-+-|-+-+-+-+-+-+-+-|-... 出力バイト列のビットの区切り
         * 0               1               2               3               4
         * </pre>
         */
        switch (input.length() % 8) {
        /*
         * 出力バイト列の長さが 0 (mod 5) のときは入力文字列の長さは 0 (mod 8)。
         * 出力バイト列の長さが 1 (mod 5) のときは入力文字列の長さは 2 (mod 8)。
         * 出力バイト列の長さが 2 (mod 5) のときは入力文字列の長さは 4 (mod 8)。
         * 出力バイト列の長さが 3 (mod 5) のときは入力文字列の長さは 5 (mod 8)。
         * 出力バイト列の長さが 4 (mod 5) のときは入力文字列の長さは 7 (mod 8)。
         */
        case 1:
        case 3:
        case 6:
            throw new MyRuleException("Invalid input length ( " + input.length() + " ).");
        case 2:
            final byte remain2 = (byte) (getValue(input.charAt(input.length() - 1)) & 0x3);
            if (remain2 != 0) {
                throw new MyRuleException("Cannot use the lower bits of last character ( " + input.charAt(input.length() - 1) + " ).");
            }
            break;
        case 4:
            final byte remain4 = (byte) (getValue(input.charAt(input.length() - 1)) & 0xf);
            if (remain4 != 0) {
                throw new MyRuleException("Cannot use the lower bits of last character ( " + input.charAt(input.length() - 1) + " ).");
            }
            break;
        case 5:
            final byte remain5 = (byte) (getValue(input.charAt(input.length() - 1)) & 0x1);
            if (remain5 != 0) {
                throw new MyRuleException("Cannot use the lower bits of last character ( " + input.charAt(input.length() - 1) + " ).");
            }
            break;
        case 7:
            final byte remain7 = (byte) (getValue(input.charAt(input.length() - 1)) & 0x7);
            if (remain7 != 0) {
                throw new MyRuleException("Cannot use the lower bits of last character ( " + input.charAt(input.length() - 1) + " ).");
            }
            break;
        }

        final byte[] output = new byte[input.length() * 5 / 8];
        for (int writeIndex = 0; writeIndex < output.length; writeIndex++) {
            final int readIndex = (writeIndex * 8) / 5;
            switch (writeIndex % 5) {
            case 0:
                output[writeIndex] = (byte) ((getValue(input.charAt(readIndex)) << 3) + (getValue(input.charAt(readIndex + 1)) >> 2));
                break;
            case 1:
                output[writeIndex] = (byte) ((getValue(input.charAt(readIndex)) << 6) + (getValue(input.charAt(readIndex + 1)) << 1)
                        + (getValue(input.charAt(readIndex + 2)) >> 4));
                break;
            case 2:
                output[writeIndex] = (byte) ((getValue(input.charAt(readIndex)) << 4) + (getValue(input.charAt(readIndex + 1)) >> 1));
                break;
            case 3:
                output[writeIndex] = (byte) ((getValue(input.charAt(readIndex)) << 7) + (getValue(input.charAt(readIndex + 1)) << 2)
                        + (getValue(input.charAt(readIndex + 2)) >> 3));
                break;
            default:
                output[writeIndex] = (byte) ((getValue(input.charAt(readIndex)) << 5) + getValue(input.charAt(readIndex + 1)));
                break;
            }
        }
        return output;
    }

    @SuppressWarnings("javadoc")
    public static void main(final String[] args) {
        for (int i = 0; i < 256; i++) {
            System.out.println(toBase32(new byte[] { (byte) i }));
        }
    }

}
