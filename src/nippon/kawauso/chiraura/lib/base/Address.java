package nippon.kawauso.chiraura.lib.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.Hexadecimal;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.math.BigIntegerFunctions;

/**
 * 論理位置の指定子。
 * @author chirauraNoSakusha
 */
public final class Address implements Comparable<Address>, BytesConvertible {

    /**
     * ビット数。
     */
    public static final int SIZE = 160; // SHA1 に合わせた。

    /**
     * バイト数。
     */
    static final int BYTE_SIZE = (SIZE + Byte.SIZE - 1) / Byte.SIZE;

    // 法。
    private static final BigInteger MODULUS = BigInteger.ZERO.setBit(SIZE);

    /**
     * 論理位置の原点。
     */
    public static final Address ZERO = new Address(BigInteger.ZERO);

    /**
     * 論理位置の最大値。
     */
    public static final Address MAX = new Address(BigInteger.ONE.shiftLeft(SIZE).subtract(BigInteger.ONE));

    /**
     * 文字列にするときに省略しない長さ。
     */
    private static final int STRING_THRESHOLD_BYTE_LENGTH = 4;

    private final BigInteger value;

    /**
     * BigInteger から作成する。
     * @param value 元にする値
     * @param bitSize value の生成機構のビット数
     */
    public Address(final BigInteger value, final int bitSize) {
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

    private Address(final BigInteger value) {
        this.value = value.mod(MODULUS);
    }

    private Address(final byte[] bytes) {
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

    /**
     * 2のべき乗を加える。
     * @param exponent 指数
     * @return 2^exponent を加えた値
     */
    public Address addPowerOfTwo(final int exponent) {
        return new Address(this.value.add(BigInteger.ZERO.setBit(exponent)));
    }

    /**
     * 1 引く。
     * @return 1 引いた値
     */
    public Address subtractOne() {
        return new Address(this.value.subtract(BigInteger.ONE));
    }

    /**
     * 終点までの距離を計算する。
     * @param destination 終点
     * @return 終点までの距離
     */
    public Address distanceTo(final Address destination) {
        return new Address(destination.value.subtract(this.value));
    }

    /**
     * 割る。
     * @param divisor 除数
     * @return 商
     */
    public Address divide(final int divisor) {
        return new Address(this.value.divide(BigInteger.valueOf(divisor)));
    }

    /**
     * 立っている最上位ビットの位置を返す。
     * 立っているビットがある場合は 0 以上の値が返る。
     * 例えば、値が 1 なら 0、値が 2 なら 1。
     * @return 立っている最上位ビットの位置。
     *         立っているビットがない場合は -1
     */
    public int highestSetBit() {
        return BigIntegerFunctions.highestSetBit(this.value);
    }

    /**
     * 立っているビットの数を返す。
     * @return 立っているビットの数
     */
    public int bitCount() {
        return this.value.bitCount();
    }

    private static final byte[] REVERSED_BYTE;

    static {
        REVERSED_BYTE = new byte[1 << Byte.SIZE];
        for (int i = 0; i < REVERSED_BYTE.length; i++) {
            byte b = 0;
            for (int j = 0; j < Byte.SIZE; j++) {
                if ((i & (1 << j)) != 0) {
                    b |= 1 << (Byte.SIZE - 1 - j);
                }
            }
            REVERSED_BYTE[i] = b;
        }
    }

    /**
     * ビットを逆順に並べた値を足す。
     * @param v ビットを逆順にする値
     * @return ビットを逆順に並べた値を足した論理位置
     */
    public Address addReverseBits(final int v) {
        final byte[] buff = new byte[BYTE_SIZE];
        for (int i = 0, n = Math.min((Integer.SIZE + Byte.SIZE - 1) / Byte.SIZE, BYTE_SIZE); i < n; i++) {
            buff[i] = REVERSED_BYTE[(v >> (Byte.SIZE * i)) & ((1 << Byte.SIZE) - 1)];
        }
        return new Address(this.toBigInteger().add(new BigInteger(buff)));
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
        } else if (buff.length == BYTE_SIZE + 1) {
            output.write(buff, 1, BYTE_SIZE);
        } else {
            output.write(buff);
        }
        return BYTE_SIZE;
    }

    /**
     * 読み込み器を返す。
     * @return 読み込み器
     */
    public static BytesConvertible.Parser<Address> getParser() {
        return new BytesConvertible.Parser<Address>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super Address> output) throws MyRuleException, IOException {
                if (maxByteSize < BYTE_SIZE) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                output.add(new Address(StreamFunctions.completeRead(input, BYTE_SIZE)));
                return BYTE_SIZE;
            }
        };
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Address)) {
            return false;
        }
        final Address other = (Address) obj;
        return this.value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public int compareTo(final Address o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName())).append('[');
        final byte[] bytes = BytesConversion.toBytes(this);
        if (bytes.length <= STRING_THRESHOLD_BYTE_LENGTH) {
            buff.append(Hexadecimal.toHexadecimal(bytes));
        } else {
            buff.append(Hexadecimal.toHexadecimal(Arrays.copyOf(bytes, STRING_THRESHOLD_BYTE_LENGTH))).append("...");
        }
        return buff.append(']').toString();
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
    public static Address fromHexadecimal(final String input) throws MyRuleException {
        if (input.length() != BYTE_SIZE * 2) {
            throw new MyRuleException("Invalid input length:" + input.length() + " required:" + (BYTE_SIZE * 2) + ".");
        }
        return new Address(Hexadecimal.fromHexadecimal(input));
    }

    /**
     * BigInteger に変換する。
     * @return この論理位置を表す BigInteger
     */
    public BigInteger toBigInteger() {
        return this.value;
    }

}