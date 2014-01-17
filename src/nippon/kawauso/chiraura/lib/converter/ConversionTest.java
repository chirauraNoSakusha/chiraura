package nippon.kawauso.chiraura.lib.converter;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;

/**
 * @author chirauraNoSakusha
 */
public final class ConversionTest {

    private static final Logger LOG = Logger.getLogger(ConversionTest.class.getName());

    private static <T> void assertEquals(final T o1, final T o2) {
        if (!o1.getClass().isArray()) {
            Assert.assertEquals(o1, o2);
        } else if (o1 instanceof byte[]) {
            Assert.assertArrayEquals((byte[]) o1, (byte[]) o2);
        } else if (o1 instanceof short[]) {
            Assert.assertArrayEquals((short[]) o1, (short[]) o2);
        } else if (o1 instanceof int[]) {
            Assert.assertArrayEquals((int[]) o1, (int[]) o2);
        } else if (o1 instanceof long[]) {
            Assert.assertArrayEquals((long[]) o1, (long[]) o2);
        } else if (o1 instanceof float[]) {
            Assert.assertArrayEquals((float[]) o1, (float[]) o2, 0.0F);
        } else if (o1 instanceof double[]) {
            Assert.assertArrayEquals((double[]) o1, (double[]) o2, 0.0);
        } else if (o1 instanceof char[]) {
            Assert.assertArrayEquals((char[]) o1, (char[]) o2);
        } else if (o1 instanceof boolean[]) {
            // 失敗するだろう。
            Assert.assertEquals(o1, o2);
        } else {
            Assert.assertEquals(o1, o2);
        }
    }

    /**
     * @author chirauraNoSakusha
     * @param <F> 変換前のクラス
     * @param <T> 変換後のクラス
     */
    public static interface Converter<F, T> {
        /**
         * @param from 変換前
         * @return 変換後
         * @throws Exception エラー
         */
        public T convert(F from) throws Exception;
    }

    private static <F, T> int testOneWay(final F from, final Converter<F, T> converter, final T answer) throws Exception {
        final T to = converter.convert(from);
        assertEquals(answer, to);
        return to.hashCode();
    }

    /**
     * @param <F> 変換前のクラス
     * @param <T> 変換後のクラス
     * @param froms 変換元
     * @param converter 変換器
     * @param answers 答え
     * @return 変換後のハッシュ値
     * @throws Exception エラー
     */
    public static <F, T> long testOneWay(final F[] froms, final Converter<F, T> converter, final T[] answers) throws Exception {
        long sum = 0;
        for (int i = 0; i < froms.length; i++) {
            sum += testOneWay(froms[i], converter, answers[i]);
        }
        return sum;
    }

    private static <F, T> int testEachOther(final F from, final Converter<F, T> converter, final Converter<T, F> reverseConverter) throws Exception {
        final F copy = reverseConverter.convert(converter.convert(from));
        assertEquals(from, copy);
        return copy.hashCode();
    }

    /**
     * @param <F> 変換前のクラス
     * @param <T> 変換後のクラス
     * @param froms 変換前のリスト
     * @param converter 変換器
     * @param reverseConverter 逆変換器
     * @return 変換後のハッシュ値の合計
     * @throws Exception エラー
     */
    public static <F, T> long testEachOther(final F[] froms, final Converter<F, T> converter, final Converter<T, F> reverseConverter) throws Exception {
        long sum = 0;
        for (final F from : froms) {
            sum += testEachOther(from, converter, reverseConverter);
        }
        return sum;
    }

    /**
     * @author chirauraNoSakusha
     * @param <F> 生成するクラス
     */
    public static interface Generator<F> {
        /**
         * @param seed 生成種
         * @return 生成物
         * @throws Exception エラー
         */
        F next(int seed) throws Exception;
    }

    private static <F, T> int testException(final F from, final Converter<F, T> converter, final Set<Class<? extends Exception>> permitted) throws Exception {
        T to = null;
        try {
            to = converter.convert(from);
        } catch (final Exception e) {
            if (!permitted.contains(e.getClass())) {
                throw e;
            }
        }
        return (to == null ? 0 : to.hashCode());
    }

    /**
     * @param <F> 変換前のクラス
     * @param <T> 変換後のクラス
     * @param generator 入力の生成器
     * @param converter 変換器
     * @param permitted 許容するエラー
     * @param loop 繰り返し回数
     * @throws Exception エラー
     */
    public static <F, T> void testException(final Generator<F> generator, final Converter<F, T> converter, final Set<Class<? extends Exception>> permitted,
            final int loop) throws Exception {
        final long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < loop; i++) {
            sum += testException(generator.next(i), converter, permitted);
        }
        final long end = System.nanoTime();
        LOG.log(Level.SEVERE, "繰り返し:" + loop + " 秒数:" + ((end - start) / 1000000000.0) + " チェックサム:" + sum);
    }

    /**
     * @param <F> 変換前のクラス
     * @param <T> 変換後のクラス
     * @param generator 入力の生成器
     * @param converter 変換器
     * @param reverseConverter 逆変換器
     * @param loop 繰り返し回数
     * @throws Exception エラー
     */
    public static <F, T> void testPerformance(final Generator<F> generator, final Converter<F, T> converter, final Converter<T, F> reverseConverter,
            final int loop) throws Exception {
        final long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < loop; i++) {
            sum += testEachOther(generator.next(i), converter, reverseConverter);
        }
        final long end = System.nanoTime();
        LOG.log(Level.SEVERE, "繰り返し:" + loop + " 秒数:" + ((end - start) / 1_000_000_000.0) + " チェックサム:" + sum);
    }

}
