package nippon.kawauso.chiraura.lib.converter;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.test.RandomString;
import nippon.kawauso.chiraura.lib.test.TestFunctions;

import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class HexadecimalTest {

    /**
     * 初期化。
     */
    public HexadecimalTest() {
        TestFunctions.testLogging(this.getClass().getName());
    }

    private static final byte[][] ogiginalList = new byte[][] { new byte[] { 0, 1, 2, 3, 4 }, new byte[] {}, new byte[] { 127, (byte) 128, (byte) 129 } };

    private static final ConversionTest.Converter<byte[], String> converter = new ConversionTest.Converter<byte[], String>() {
        @Override
        public String convert(final byte[] from) throws Exception {
            return Hexadecimal.toHexadecimal(from);
        }
    };
    private static final ConversionTest.Converter<String, byte[]> reverseConverter = new ConversionTest.Converter<String, byte[]>() {
        @Override
        public byte[] convert(final String from) throws Exception {
            return Hexadecimal.fromHexadecimal(from);
        }
    };

    private static final Set<Class<? extends Exception>> permitted = new HashSet<>();
    static {
        permitted.add(MyRuleException.class);
    }

    private final ConversionTest.Generator<String> randomStringGenerator = new ConversionTest.Generator<String>() {
        final static int maxLength = 100;
        final Random random = new Random(System.nanoTime());

        @Override
        public String next(final int seed) throws Exception {
            return RandomString.nextString(this.random.nextInt(maxLength), this.random);
        }
    };

    private final ConversionTest.Generator<byte[]> generator = new ConversionTest.Generator<byte[]>() {
        final static int length = 100;

        @Override
        public byte[] next(final int seed) throws Exception {
            final byte[] bytes = new byte[length];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (seed + i);
            }
            return bytes;
        }
    };

    /**
     * サンプルで変換検査。
     * @throws Exception エラー
     */

    @Test
    public void testConversion() throws Exception {
        ConversionTest.testEachOther(ogiginalList, converter, reverseConverter);
    }

    /**
     * @throws Exception エラー
     */
    @Test
    public void testException() throws Exception {
        ConversionTest.testException(this.randomStringGenerator, reverseConverter, permitted, 100_000);
    }

    /**
     * @throws Exception エラー
     */
    @Test
    public void testPerformance() throws Exception {
        ConversionTest.testPerformance(this.generator, converter, reverseConverter, 100_000);
    }

}
