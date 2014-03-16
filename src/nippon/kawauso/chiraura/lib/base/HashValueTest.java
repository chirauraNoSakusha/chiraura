/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.base;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;
import nippon.kawauso.chiraura.lib.converter.ConversionTest;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.test.RandomString;
import nippon.kawauso.chiraura.lib.test.TestFunctions;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class HashValueTest extends BytesConvertibleTest<HashValue> {

    /**
     * 初期化。
     */
    public HashValueTest() {
        TestFunctions.testLogging(this.getClass().getSimpleName());
    }

    /**
     * 入力が分割されていても同じ値になるかどうかの検査。
     */

    @Test
    public void testConsistency() {
        Assert.assertEquals(HashValue.calculateFromBytes(new byte[] { 1, 2, 3, 4, 5, 6 }),
                HashValue.calculateFromBytes(new byte[] { 1, 2, 3, 4 }, new byte[] { 5, 6 }));
        Assert.assertEquals(HashValue.calculateFromString("いろはにほへと"), HashValue.calculateFromString("い", "ろはにほ", "へと"));
    }

    private static final HashValue[] originalList;
    static {
        final List<HashValue> buff = new ArrayList<>();
        buff.add(new HashValue(BigInteger.valueOf(12414).abs(), Integer.SIZE));
        buff.add(new HashValue(BigInteger.valueOf(12414164679L).abs(), Long.SIZE));
        for (int i = 0; i < 5; i++) {
            buff.add(HashValue.calculateFromBytes(new byte[] { (byte) i }));
        }
        originalList = buff.toArray(new HashValue[0]);
    }

    private static final ConversionTest.Converter<HashValue, String> stringConverter = new ConversionTest.Converter<HashValue, String>() {
        @Override
        public String convert(final HashValue from) throws Exception {
            return from.toString();
        }
    };

    private final static ConversionTest.Converter<String, HashValue> stringReverseConverter = new ConversionTest.Converter<String, HashValue>() {
        @Override
        public HashValue convert(final String from) throws Exception {
            return HashValue.fromString(from);
        }
    };

    private final static ConversionTest.Generator<HashValue> generator = new ConversionTest.Generator<HashValue>() {
        final byte[] hashSeed = new byte[10];

        @Override
        public HashValue next(final int seed) throws Exception {
            for (int j = 0; j < this.hashSeed.length; j++) {
                this.hashSeed[j] = (byte) (seed + j);
            }
            return HashValue.calculateFromBytes(this.hashSeed);
        }
    };

    private final static ConversionTest.Generator<String> randomGenerator = new ConversionTest.Generator<String>() {
        final Random random = new Random(System.nanoTime());
        final int maxLength = 100;

        @Override
        public String next(final int seed) throws Exception {
            return RandomString.nextString(this.random.nextInt(this.maxLength), this.random);
        }
    };

    private static final Set<Class<? extends Exception>> permitted = new HashSet<>();
    static {
        permitted.add(MyRuleException.class);
    }

    /**
     * @throws Exception エラー
     */

    @Test
    public void testStringConversion() throws Exception {
        ConversionTest.testEachOther(originalList, stringConverter, stringReverseConverter);
    }

    /**
     * @throws Exception 想定外のエラー
     */
    @Test
    public void testStringConversionException() throws Exception {
        ConversionTest.testException(randomGenerator, stringReverseConverter, permitted, getNumOfLoops());
    }

    /**
     * @throws Exception エラー
     */
    @Test
    public void testStringConversionPerformance() throws Exception {
        ConversionTest.testPerformance(generator, stringConverter, stringReverseConverter, getNumOfLoops());
    }

    private static final ConversionTest.Converter<HashValue, String> base64Converter = new ConversionTest.Converter<HashValue, String>() {
        @Override
        public String convert(final HashValue from) throws Exception {
            return from.toBase64();
        }
    };

    private final static ConversionTest.Converter<String, HashValue> base64ReverseConverter = new ConversionTest.Converter<String, HashValue>() {
        @Override
        public HashValue convert(final String from) throws Exception {
            return HashValue.fromBase64(from);
        }
    };

    /**
     * @throws Exception エラー
     */

    @Test
    public void testBase64Conversion() throws Exception {
        ConversionTest.testEachOther(originalList, base64Converter, base64ReverseConverter);
    }

    /**
     * @throws Exception 想定外のエラー
     */
    @Test
    public void testBase64ConversionException() throws Exception {
        ConversionTest.testException(randomGenerator, base64ReverseConverter, permitted, getNumOfLoops());
    }

    /**
     * @throws Exception エラー
     */
    @Test
    public void testBase64ConversionPerformance() throws Exception {
        ConversionTest.testPerformance(generator, base64Converter, base64ReverseConverter, getNumOfLoops());
    }

    @Override
    protected HashValue[] getInstances() {
        return originalList;
    }

    @Override
    protected BytesConvertible.Parser<HashValue> getParser() {
        return HashValue.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

    /**
     * 試験用に作成する。
     * @param seed 種
     * @return 試験用インスタンス
     */
    public static HashValue newInstance(final int seed) {
        return new HashValue(BigInteger.valueOf(seed).abs(), Integer.SIZE);
    }

    @Override
    protected HashValue getInstance(final int seed) {
        return new HashValue(BigInteger.valueOf(seed).abs(), Integer.SIZE);
    }

}
