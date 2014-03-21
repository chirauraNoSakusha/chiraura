/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.converter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.test.TestFunctions;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 * @param <T> テストするクラス
 */
@Ignore
public abstract class BytesConvertibleTest<T extends BytesConvertible> {

    private static final Logger LOG = Logger.getLogger(BytesConvertibleTest.class.getName());

    /**
     * 初期化
     */
    public BytesConvertibleTest() {
        TestFunctions.testLogging(this.getClass().getName());
        // LoggingFunctions.startDebugLogging();
    }

    private static final int PIPE_SIZE = 100 * 1024 * 1024; // 100MB.

    /**
     * 変換テストに使用するインスタンスを得る。
     * @return インスタンスの配列
     */
    protected abstract T[] getInstances();

    /**
     * 性能検査内で使用するインスタンスを得る。
     * @param seed 生成種
     * @return インスタンス
     */
    protected abstract T getInstance(int seed);

    protected T getPerformanceInstance(final int seed) {
        return getInstance(seed);
    }

    /**
     * 復号器を得る。
     * @return 復号器
     */
    protected abstract BytesConvertible.Parser<T> getParser();

    protected BytesConvertible.Parser<T> getPerformanceParser(@SuppressWarnings("unused") final int seed) {
        return getParser();
    }

    protected BytesConvertible.Parser<T> getExceptionParser(@SuppressWarnings("unused") final int seed) {
        return getParser();
    }

    /**
     * エラー検査や性能検査内での繰り返し回数を得る。
     * @return 繰り返し回数
     */
    protected abstract int getNumOfLoops();

    protected int getNumOfPerformanceLoops() {
        return getNumOfLoops();
    }

    protected int getNumOfExceptionLoops() {
        return getNumOfLoops();
    }

    /**
     * エラー検査での最大読み込みバイト数。
     * @return 最大読み込みバイト数
     */
    protected int getExceptionLength() {
        return 100;
    }

    /**
     * 変換テスト。
     * @throws MyRuleException 変換規約に違反している場合
     * @throws IOException エラー
     */
    @Test
    public void testConversion() throws MyRuleException, IOException {
        final T[] testInstances = getInstances();

        int size = 0;
        for (final T instance : testInstances) {
            size += instance.byteSize();
        }

        final PipedOutputStream rawOutputStream = new PipedOutputStream();
        final PipedInputStream rawInputStream = new PipedInputStream(rawOutputStream, PIPE_SIZE);
        final OutputStream outputStream = new BufferedOutputStream(rawOutputStream);
        final InputStream inputStream = new BufferedInputStream(rawInputStream);

        int writeSize = 0;
        for (final T instance : testInstances) {
            final int s;
            try {
                s = instance.toStream(outputStream);
            } catch (final Exception e) {
                System.err.println("Error instance: " + instance);
                inputStream.close();
                throw e;
            }
            Assert.assertEquals(instance.byteSize(), s);
            writeSize += s;
        }
        outputStream.close();
        Assert.assertEquals(size, writeSize);

        int readSize = 0;
        final List<T> output = new ArrayList<>(1);
        for (final T instance : testInstances) {
            try {
                readSize += getParser().fromStream(inputStream, instance.byteSize(), output);
            } catch (final Exception e) {
                System.err.println("Error instance: " + instance);
                throw e;
            }
            Assert.assertEquals(instance, output.remove(0));
        }
        Assert.assertTrue(StreamFunctions.isEof(inputStream));
        inputStream.close();
        Assert.assertEquals(size, readSize);
    }

    /**
     * fromStream(InputStream) において想定外のエラーが発生しないかどうかの検査。
     * @throws Exception 想定外のエラー
     */
    @Test
    public void testException() throws Exception {
        final long randomSeed = System.nanoTime();
        final Random random = new Random(randomSeed);
        final int loop = getNumOfExceptionLoops();
        final int maxLength = getExceptionLength();

        long finishCount = 0;
        long sum = 0;

        byte[] buff = new byte[random.nextInt(maxLength)];

        final long start = System.nanoTime();

        for (int i = 0; i < loop; i++) {
            if (i > 100 && i % (loop / 100) == 0) {
                buff = new byte[random.nextInt(maxLength)];
            }
            random.nextBytes(buff);
            final int maxByteSize = random.nextInt(maxLength);
            final InputStream inputStream = new ByteArrayInputStream(buff);
            try {
                int size = 0;
                while (!StreamFunctions.isEof(inputStream) && i < loop) {
                    final List<T> output = new ArrayList<>(1);
                    try {
                        size += getExceptionParser(i).fromStream(inputStream, maxByteSize - size, output);
                    } catch (final MyRuleException ignored) {
                        // このときは for で i がインクリメントされる。
                        break;
                    }
                    i++;
                    finishCount++;
                    sum += output.get(0).hashCode();
                }
            } catch (final Exception e) {
                System.err.println("Loop count: " + i + ", Random seed: " + randomSeed);
                throw e;
            }
        }
        final long end = System.nanoTime();

        final double unitCost = (end - start) / (1_000_000.0 * loop);
        if (unitCost > 1.0) {
            LOG.log(Level.SEVERE, "{0} 単位当たり {1} ミリ秒も掛かってます。", new Object[] { this.getClass().getName(), String.format("%f", unitCost) });
        }
        LOG.log(Level.SEVERE, "{0} 繰り返し回数: {1} 最大読み込み長: {2} 単位消費ミリ秒数: {3} 正常終了回数: {4} チェックサム: {5}",
                new Object[] { this.getClass().getName(), loop, maxLength, String.format("%f", unitCost), finishCount, sum });
    }

    /**
     * toStream(OutputStream) と fromStream(InputStream) による変換性能を検査。
     * @throws MyRuleException 変換規約に違反している場合
     * @throws Exception エラー
     */
    @Test
    public void testPerformance() throws MyRuleException, Exception {
        final int loop = getNumOfPerformanceLoops();

        long sum = 0;

        final PipedOutputStream rawOutputStream = new PipedOutputStream();
        final PipedInputStream rawInputStream = new PipedInputStream(rawOutputStream, PIPE_SIZE);
        final OutputStream outputStream = new BufferedOutputStream(rawOutputStream);
        final InputStream inputStream = new BufferedInputStream(rawInputStream);

        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            final T instance = getPerformanceInstance(i);
            try {
                final int writeSize = instance.toStream(outputStream);
                outputStream.flush();

                final List<T> output = new ArrayList<>(1);
                final int readSize = getPerformanceParser(i).fromStream(inputStream, writeSize, output);

                Assert.assertEquals(writeSize, readSize);
                Assert.assertEquals(instance, output.get(0));

                sum += output.get(0).hashCode();
            } catch (final Exception e) {
                System.err.println("Instance: " + instance);
                System.err.println("Loop count (seed): " + i);
                throw e;
            }
        }
        final long end = System.nanoTime();

        final double unitCost = (end - start) / (1_000_000.0 * loop);
        if (unitCost > 1.0) {
            LOG.log(Level.SEVERE, "{0} 単位当たり {1} ミリ秒も掛かってます。", new Object[] { this.getClass().getName(), String.format("%f", unitCost) });
        }
        LOG.log(Level.SEVERE, "{0} 繰り返し回数: {1} 単位消費ミリ秒数: {2} チェックサム: {3}",
                new Object[] { this.getClass().getName(), loop, String.format("%f", unitCost), sum });
    }

}
