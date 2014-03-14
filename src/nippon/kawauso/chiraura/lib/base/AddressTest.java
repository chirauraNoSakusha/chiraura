/**
 * 
 */
package nippon.kawauso.chiraura.lib.base;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;
import nippon.kawauso.chiraura.lib.test.RandomBigInteger;

/**
 * @author chirauraNoSakusha
 */
public final class AddressTest extends BytesConvertibleTest<Address> {

    /**
     * ランダムな論理位置を生成する。
     * @param random 乱数生成器
     * @return ランダムな論理位置
     */
    public static Address newRandomInstance(final Random random) {
        return new Address(RandomBigInteger.positive(random, Address.SIZE), Address.SIZE);
    }

    /**
     * 試験用に生成する。
     * @param seed 種
     * @return 試験用インスタンス
     */
    public static Address newInstance(final int seed) {
        return Address.ZERO.addReverseBits(seed);
    }

    @Override
    protected Address[] getInstances() {
        final List<Address> list = new ArrayList<>();
        list.add(new Address(BigInteger.valueOf(12414), Integer.SIZE));
        list.add(new Address(BigInteger.valueOf(12414164679L), HashValue.SIZE));
        return list.toArray(new Address[0]);
    }

    @Override
    protected Address getInstance(final int seed) {
        return new Address(BigInteger.valueOf(seed).abs(), Integer.SIZE);
    }

    @Override
    protected BytesConvertible.Parser<Address> getParser() {
        return Address.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

}
