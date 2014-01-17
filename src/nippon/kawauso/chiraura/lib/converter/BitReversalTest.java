package nippon.kawauso.chiraura.lib.converter;

import java.math.BigInteger;
import java.util.Random;

import nippon.kawauso.chiraura.lib.test.TestFunctions;

import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class BitReversalTest {

    /**
     * 初期化。
     */
    public BitReversalTest() {
        TestFunctions.testLogging(this.getClass().getName());
    }

    /*--------------------------------------------------------------------------------
     * byte
     *--------------------------------------------------------------------------------*/
    private static final int byteLoop = 1_000_000;
    private static final Byte[] byteList = new Byte[] { 30, (byte) Integer.MIN_VALUE, (byte) -2145145 };

    private static final ConversionTest.Converter<Byte, Byte> byteConverter = new ConversionTest.Converter<Byte, Byte>() {
        @Override
        public Byte convert(final Byte from) throws Exception {
            return BitReversal.getByte(from);
        }
    };

    private final ConversionTest.Generator<Byte> byteGenerator = new ConversionTest.Generator<Byte>() {
        private final byte offset = (byte) System.nanoTime();

        @Override
        public Byte next(final int seed) throws Exception {
            return (byte) (BitReversal.getByte((byte) seed) + this.offset);
        }
    };

    /**
     * サンプルで変換検査。
     * @throws Exception エラー
     */
    
    @Test
    public void testByteConversion() throws Exception {
        ConversionTest.testEachOther(byteList, byteConverter, byteConverter);
    }

    /**
     * @throws Exception エラー
     */
    @Test
    public void testBytePerformance() throws Exception {
        ConversionTest.testPerformance(this.byteGenerator, byteConverter, byteConverter, byteLoop);
    }

    /*--------------------------------------------------------------------------------
     * short
     *--------------------------------------------------------------------------------*/
    private static final int shortLoop = 1_000_000;
    private static final Short[] shortList = new Short[] { 30, (short) Integer.MIN_VALUE, (short) -2145145 };

    private static final ConversionTest.Converter<Short, Short> shortConverter = new ConversionTest.Converter<Short, Short>() {
        @Override
        public Short convert(final Short from) throws Exception {
            return BitReversal.getShort(from);
        }
    };

    private final ConversionTest.Generator<Short> shortGenerator = new ConversionTest.Generator<Short>() {
        private final short offset = (short) System.nanoTime();

        @Override
        public Short next(final int seed) throws Exception {
            return (short) (BitReversal.getShort((short) seed) + this.offset);
        }
    };

    /**
     * サンプルで変換検査。
     * @throws Exception エラー
     */
    
    @Test
    public void testShortConversion() throws Exception {
        ConversionTest.testEachOther(shortList, shortConverter, shortConverter);
    }

    /**
     * @throws Exception エラー
     */
    @Test
    public void testShortPerformance() throws Exception {
        ConversionTest.testPerformance(this.shortGenerator, shortConverter, shortConverter, shortLoop);
    }

    /*--------------------------------------------------------------------------------
     * int
     *--------------------------------------------------------------------------------*/
    private static final int intLoop = 1_000_000;
    private static final Integer[] intList = new Integer[] { 30, (int) Integer.MIN_VALUE, -2145145 };

    private static final ConversionTest.Converter<Integer, Integer> intConverter = new ConversionTest.Converter<Integer, Integer>() {
        @Override
        public Integer convert(final Integer from) throws Exception {
            return BitReversal.getInt(from);
        }
    };

    private final ConversionTest.Generator<Integer> intGenerator = new ConversionTest.Generator<Integer>() {
        private final int offset = (int) System.nanoTime();

        @Override
        public Integer next(final int seed) throws Exception {
            return BitReversal.getInt(seed) + this.offset;
        }
    };

    /**
     * サンプルで変換検査。
     * @throws Exception エラー
     */
    
    @Test
    public void testIntConversion() throws Exception {
        ConversionTest.testEachOther(intList, intConverter, intConverter);
    }

    /**
     * @throws Exception エラー
     */
    @Test
    public void testIntPerformance() throws Exception {
        ConversionTest.testPerformance(this.intGenerator, intConverter, intConverter, intLoop);
    }

    /*--------------------------------------------------------------------------------
     * long
     *--------------------------------------------------------------------------------*/
    private static final int longLoop = 1_000_000;
    private static final Long[] longList = new Long[] { 30L, (long) Integer.MIN_VALUE, -2145145L };

    private static final ConversionTest.Converter<Long, Long> longConverter = new ConversionTest.Converter<Long, Long>() {
        @Override
        public Long convert(final Long from) throws Exception {
            return BitReversal.getLong(from);
        }
    };

    private final ConversionTest.Generator<Long> longGenerator = new ConversionTest.Generator<Long>() {
        private final long offset = System.nanoTime();

        @Override
        public Long next(final int seed) throws Exception {
            return BitReversal.getLong(seed) + this.offset;
        }
    };

    /**
     * サンプルで変換検査。
     * @throws Exception エラー
     */
    
    @Test
    public void testLongConversion() throws Exception {
        ConversionTest.testEachOther(longList, longConverter, longConverter);
    }

    /**
     * @throws Exception エラー
     */
    @Test
    public void testLongPerformance() throws Exception {
        ConversionTest.testPerformance(this.longGenerator, longConverter, longConverter, longLoop);
    }

    /*--------------------------------------------------------------------------------
     * BigInteger
     *--------------------------------------------------------------------------------*/
    private static final int bigIntegerLoop = 100_000;
    private static final BigInteger[] bigIntegerList = new BigInteger[] { BigInteger.valueOf(30), BigInteger.valueOf(Integer.MIN_VALUE),
            BigInteger.valueOf(-2145145) };

    private static ConversionTest.Converter<BigInteger, BigInteger> getBigIntegerConverter(final int numOfBit) {
        return new ConversionTest.Converter<BigInteger, BigInteger>() {
            @Override
            public BigInteger convert(final BigInteger from) throws Exception {
                return BitReversal.getBigInteger(from, numOfBit);
            }
        };
    }

    private static ConversionTest.Converter<BigInteger, BigInteger> getOldBigIntegerConverter(final int numOfBit) {
        return new ConversionTest.Converter<BigInteger, BigInteger>() {
            @Override
            public BigInteger convert(final BigInteger from) throws Exception {
                return BitReversal.getOldBigInteger(from, numOfBit);
            }
        };
    }

    private static ConversionTest.Generator<BigInteger> getBigIntegerGenerator(final int numOfBits) {
        return new ConversionTest.Generator<BigInteger>() {
            private final int MAX_LENGTH = (numOfBits + 7 / 8);
            private final BigInteger BOUND = BigInteger.ONE.shiftLeft(numOfBits - 1 /* 符号分 */);
            private final Random random = new Random(System.nanoTime());

            @Override
            public BigInteger next(final int seed) throws Exception {
                final byte[] bytes = new byte[1 + this.random.nextInt(Math.max(0, this.MAX_LENGTH - 1))];
                this.random.nextBytes(bytes);
                return new BigInteger(bytes).mod(this.BOUND);
            }
        };
    }

    /**
     * サンプルで変換検査。
     * @throws Exception エラー
     */
    
    @Test
    public void testBigIntegerConversion() throws Exception {
        ConversionTest.testEachOther(bigIntegerList, getBigIntegerConverter(40), getBigIntegerConverter(40));
    }

    /**
     * @throws Exception エラー
     */
    
    @Test
    public void testBigIntegerPerformance() throws Exception {
        for (final int numOfBits : new int[] { 10, 20, 40, 80, 160 }) {
            ConversionTest.testPerformance(getBigIntegerGenerator(numOfBits), getBigIntegerConverter(numOfBits), getBigIntegerConverter(numOfBits),
                    bigIntegerLoop);
        }
    }

    /**
     * @throws Exception エラー
     */
    
    @Test
    public void testOldBigIntegerPerformance() throws Exception {
        for (final int numOfBits : new int[] { 10, 20, 40, 80, 160 }) {
            ConversionTest.testPerformance(getBigIntegerGenerator(numOfBits), getOldBigIntegerConverter(numOfBits), getOldBigIntegerConverter(numOfBits),
                    bigIntegerLoop);
        }
    }

}
