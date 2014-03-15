package nippon.kawauso.chiraura.lib;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;

import nippon.kawauso.chiraura.lib.connection.PublicPeerCell;
import nippon.kawauso.chiraura.lib.converter.Base64;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * いやんな情報にモザイクを掛けたり取ったりする。
 * @author chirauraNoSakusha
 */
public final class Mosaic {

    // インスタンス化防止。
    private Mosaic() {}

    /*
     * 先頭バイトをマスクの開始位置。
     * 以降は入力値とマスク値の XOR 値にする。
     */

    private static final byte[] MASK = new byte[] {
            -52, -121, 63, -128, 103, 44, -76, 74,
            -34, 80, 93, -65, -96, -19, 10, 20,
            17, -111, -105, 24, -109, -17, -73, 3,
            21, 35, -73, -93, -84, 72, -118, 103,
            -25, 45, -58, 30, -69, 44, 65, 126,
            44, 126, 65, 78, 68, -104, -109, 47,
            76, -53, 61, -36, -2, 6, 25, 81,
            -54, 56, 32, -42, -3, 41, -8, 76,
            -95, -23, 46, 96, 22, -44, 6, -111,
            -68, -4, -108, 39, -45, -23, -17, -59,
            -38, -75, 38, -125, -46, -39, -70, 65,
            -61, 67, 75, 101, -4, -65, 47, -61,
            107, -26, -35, 45, 64, 104, -21, -51,
            -17, -42, 85, -38, -72, 100, -8, -37,
            -58, -30, -39, 119, 6, 113, 58, 19,
            -123, 125, 104, -101, -41, 77, 118, 4,
            -20, -121, -36, -19, 77, 110, -121, -92,
            -41, -54, 36, -84, -124, 42, 44, -115,
            -100, 113, 6, 79, 108, 24, -42, 63,
            -78, -67, 37, -111, -91, 99, 88, 89,
            -44, -14, 7, -6, -107, 91, -27, 124,
            36, 37, 69, 44, -97, -52, -74, 67,
            66, 76, 30, -63, 117, 106, -124, -64,
            19, 70, 100, 37, -64, 3, 61, -95,
            -97, 68, 27, 124, -63, 122, -47, -87,
            -13, -17, -19, 75, 89, 117, -81, -128,
            89, 76, 65, 49, -42, -35, -4, 12,
            110, 61, -25, 114, 27, -26, -107, 88,
            -52, -94, 29, -49, 109, -88, -1, -87,
            -93, 43, 40, 43, 43, -67, 39, -20,
            -64, 75, -18, 71, -54, 31, -42, -20,
            -32, 82, 19, -1, 81, -126, 123, -77,
    };

    private static byte[] encrypt(final byte[] raw) {
        final int offset = ThreadLocalRandom.current().nextInt(1 << Byte.SIZE);
        final byte[] encrypted = new byte[raw.length + 1];
        encrypted[0] = (byte) offset;
        for (int i = 0; i < raw.length; i++) {
            encrypted[1 + i] = (byte) (raw[i] ^ MASK[(offset + i) % MASK.length]);
        }
        return encrypted;
    }

    private static byte[] decrypt(final byte[] encrypted) throws MyRuleException {
        if (encrypted.length < 1) {
            throw new MyRuleException("Too short encrypted byte sequence.");
        }
        final int offset = ((1 << Byte.SIZE) - 1) & encrypted[0];
        final byte[] raw = new byte[encrypted.length - 1];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) (encrypted[i + 1] ^ MASK[(offset + i) % MASK.length]);
        }
        return raw;
    }

    /**
     * モザイクを掛ける。
     * @param raw 元のバイト列
     * @return モザイクの掛かった文字列
     */
    public static String bytesTo(final byte[] raw) {
        return Base64.toBase64(encrypt(raw));
    }

    /**
     * モザイクを取る。
     * @param mosaic モザイクの掛かった文字列
     * @return 元のバイト列
     * @throws MyRuleException 入力文字列の規約違反
     */
    public static byte[] bytesFrom(final String mosaic) throws MyRuleException {
        return decrypt(Base64.fromBase64(mosaic));
    }

    /**
     * モザイクを掛ける。
     * @param raw 元の個体
     * @return モザイクの掛かった文字列
     */
    public static String peerTo(final InetSocketAddress raw) {
        // Base64 で区切りが良いように 3 の倍数。
        final int unitLength = 15;
        final int minLength = 3 * unitLength;

        final byte[] encrypted = encrypt(BytesConversion.toBytes(new PublicPeerCell(raw)));
        final int length;
        if (encrypted.length <= minLength) {
            length = minLength;
        } else {
            length = ((encrypted.length + unitLength - 1) / unitLength) * unitLength;
        }

        final byte[] pad = new byte[length - encrypted.length];
        ThreadLocalRandom.current().nextBytes(pad);

        final byte[] buff = new byte[length];
        System.arraycopy(encrypted, 0, buff, 0, encrypted.length);
        System.arraycopy(pad, 0, buff, encrypted.length, pad.length);

        return Base64.toBase64(buff);
    }

    /**
     * モザイクを取る。
     * @param mosaic モザイクの掛かった文字列
     * @return 元の個体
     * @throws MyRuleException 入力文字列の規約違反
     */
    public static InetSocketAddress peerFrom(final String mosaic) throws MyRuleException {
        final byte[] buff = decrypt(Base64.fromBase64(mosaic));
        final PublicPeerCell peer = BytesConversion.fromBytes(buff, PublicPeerCell.getParser());
        return peer.get();
    }

}
