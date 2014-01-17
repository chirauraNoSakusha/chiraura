/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.converter.Base64;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.Hexadecimal;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 不変。
 * @author chirauraNoSakusha
 */
public final class HashValue implements Comparable<HashValue>, BytesConvertible {

    // 計算アルゴリズム。
    private static final String ALGORITHM = "SHA-1";

    // ストリームから計算する場合のバッファサイズ。
    private static final int BUFFER_SIZE = 8192;

    /**
     * バイト数。
     */
    private static final int BYTE_SIZE;
    static {
        try {
            BYTE_SIZE = MessageDigest.getInstance(ALGORITHM).getDigestLength();
        } catch (final NoSuchAlgorithmException e) {
            // 来ない。
            throw new RuntimeException(e);
        }
    }

    /**
     * ビット数。
     */
    public static final int SIZE = BYTE_SIZE * Byte.SIZE;

    /*
     * 2^SIZE 未満の正の BigInteger で表す。
     */

    private final BigInteger value;

    /**
     * 正の BigInteger から作成する。
     * @param value 元にする正の値
     * @param bitSize value の生成機構のビット数
     */
    public HashValue(final BigInteger value, final int bitSize) {
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Negative value ( " + value + " ).");
        } else if (value.bitLength() > bitSize) {
            throw new IllegalArgumentException("Too large value ( " + value + " ) over bit size ( " + bitSize + " ).");
        }
        if (bitSize == SIZE) {
            this.value = value;
        } else if (bitSize < SIZE) {
            this.value = value.shiftLeft(SIZE - bitSize);
        } else {
            this.value = value.shiftRight(bitSize - SIZE);
        }
    }

    private HashValue(final byte[] bytes) {
        if (bytes.length != BYTE_SIZE) {
            throw new IllegalArgumentException("Invalid bytes length ( " + bytes.length + " ).");
        }
        final byte[] buff;
        if (bytes[0] >= 0) {
            // 先頭ビットは 0。
            buff = bytes;
        } else {
            // 先頭ビットは 1。
            /*
             * 先頭ビットが 1 だと負の BigInteger になってしまうので、
             * 先頭に 0 のバイトを追加する。
             */
            buff = new byte[BYTE_SIZE + 1];
            System.arraycopy(bytes, 0, buff, 1, BYTE_SIZE);
        }
        this.value = new BigInteger(buff);
    }

    private static final ThreadLocal<MessageDigest> CALCULATOR = new ThreadLocal<MessageDigest>() {
        @Override
        public MessageDigest initialValue() {
            return newCalculator();
        }
    };

    /**
     * 計算機を返す。
     * newCalculator() より速いが、reset() 必須。
     * @return ハッシュ値の計算機
     */
    private static MessageDigest getCalculator() {
        return CALCULATOR.get();
    }

    /**
     * 計算機を返す。
     * @return ハッシュ値の計算機
     */
    public static MessageDigest newCalculator() {
        try {
            return MessageDigest.getInstance(ALGORITHM);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 文字列から計算する。
     * @param firstSeed 1つ目の文字列
     * @param seeds 2つ目以降の文字列
     * @return ハッシュ値
     */
    public static HashValue calculateFromString(final String firstSeed, final String... seeds) {
        final MessageDigest md = getCalculator();
        try {
            md.update(firstSeed.getBytes(Global.INTERNAL_CHARSET));
            for (final String str : seeds) {
                md.update(str.getBytes(Global.INTERNAL_CHARSET));
            }
            return new HashValue(md.digest());
        } finally {
            md.reset();
        }
    }

    /**
     * バイト列から計算する。
     * @param firstSeed 1つ目のバイト列
     * @param seeds 2つめ以降のバイト列
     * @return ハッシュ値
     */
    public static HashValue calculateFromBytes(final byte[] firstSeed, final byte[]... seeds) {
        final MessageDigest md = getCalculator();
        try {
            md.update(firstSeed);
            for (final byte[] buff : seeds) {
                md.update(buff);
            }
            return new HashValue(md.digest());
        } finally {
            md.reset();
        }
    }

    /**
     * ストリームから計算する。
     * @param seed 読み込み元
     * @return ハッシュ値
     * @throws IOException 読み込みエラー
     */
    public static HashValue calculateFromStream(final InputStream seed) throws IOException {
        final MessageDigest md = getCalculator();
        try {
            final byte[] buff = new byte[BUFFER_SIZE];
            for (int len = seed.read(buff); len > 0; len = seed.read(buff)) {
                md.update(buff, 0, len);
            }
            return new HashValue(md.digest());
        } finally {
            md.reset();
        }
    }

    /**
     * BigInteger に変換する。
     * @return 対応する BigInteger
     */
    public BigInteger toBigInteger() {
        return this.value;
    }

    @Override
    public String toString() {
        return toBase64();
    }

    /**
     * 文字列から復元する。
     * @param input 文字列
     * @return 文字列が表すハッシュ値
     * @throws MyRuleException 規約違反
     */
    public static HashValue fromString(final String input) throws MyRuleException {
        return fromBase64(input);
    }

    /**
     * 16進数の文字列に変換する。
     * @return 16進数の文字列
     */
    public String toHexadecimal() {
        return Hexadecimal.toHexadecimal(BytesConversion.toBytes(this));
    }

    /**
     * 16進数の文字列から復元する。
     * @param input 16進数の文字列
     * @return 文字列が表すハッシュ値
     * @throws MyRuleException 規約違反
     */
    public static HashValue fromHexadecimal(final String input) throws MyRuleException {
        if (input.length() != BYTE_SIZE * 2) {
            throw new MyRuleException("Input length ( " + input.length() + " ) differs required length ( " + (BYTE_SIZE * 2) + " ).");
        }
        return new HashValue(Hexadecimal.fromHexadecimal(input));
    }

    /**
     * Base64の文字列に変換する。
     * @return Base64形式の文字列
     */
    public String toBase64() {
        return Base64.toBase64(BytesConversion.toBytes(this));
    }

    /**
     * Base64の文字列から復元する。
     * @param input Base64形式の文字列
     * @return 文字列が表すハッシュ値
     * @throws MyRuleException 規約違反
     */
    public static HashValue fromBase64(final String input) throws MyRuleException {
        if (input.length() != (SIZE + 5) / 6) {
            throw new MyRuleException("Input length ( " + input.length() + " ) differs required length ( " + ((SIZE + 5) / 6) + " ).");
        }
        return new HashValue(Base64.fromBase64(input));
    }

    @Override
    public int byteSize() {
        return BYTE_SIZE;
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        final byte[] buff = this.value.toByteArray();
        if (buff.length < BYTE_SIZE) {
            output.write(new byte[BYTE_SIZE - buff.length]);
            output.write(buff);
        } else if (buff.length > BYTE_SIZE) {
            /*
             * BigInteger をバイト列にする場合、負数との混同を防ぐため、
             * 先頭に 0 のバイトが追加されることがある。
             * よって、buff.length は BYTE_SIZE + 1 になることがある。
             */
            output.write(buff, buff.length - BYTE_SIZE, BYTE_SIZE);
        } else {
            output.write(buff);
        }
        return BYTE_SIZE;
    }

    /**
     * @return 読み込み器
     */
    public static BytesConvertible.Parser<HashValue> getParser() {
        return new BytesConvertible.Parser<HashValue>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super HashValue> output) throws MyRuleException, IOException {
                if (maxByteSize < BYTE_SIZE) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                output.add(new HashValue(StreamFunctions.completeRead(input, BYTE_SIZE)));
                return BYTE_SIZE;
            }
        };
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof HashValue)) {
            return false;
        }
        final HashValue other = (HashValue) obj;
        return this.value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public int compareTo(final HashValue o) {
        return this.value.compareTo(o.value);
    }

}
