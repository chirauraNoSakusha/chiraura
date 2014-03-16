package nippon.kawauso.chiraura.lib.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.test.TestFunctions;

import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class LoggingFunctionsTest {

    private static final Logger LOG = Logger.getLogger(LoggingFunctionsTest.class.getName());

    /**
     * 初期化
     */
    public LoggingFunctionsTest() {
        TestFunctions.testLogging(this.getClass().getSimpleName());
    }

    /**
     * 単純な日時表現の性能検査。
     */
    @Test
    public void testGetSimpleDatePerformance() {
        LOG.log(Level.SEVERE, this.getClass().getName() + " 単純な日付の生成テスト");
        final int loop = 100_000;
        long sum = 0;
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            sum += LoggingFunctions.getSimpleDate(i).hashCode();
        }
        final long end = System.nanoTime();
        LOG.log(Level.SEVERE, this.getClass().getName() + " 繰り返し:" + loop + " 秒数:" + ((end - start) / 1_000_000_000.0) + " チェックサム:" + sum);
    }

    /**
     * スタックトレース文字列生成の性能検査
     */
    @Test
    public void testGetStackTraceStringPerformance() {
        LOG.log(Level.SEVERE, this.getClass().getName() + " スタックトレースの生成テスト");
        final int loop = 1_000;
        final int depth = 10;
        long sum = 0;
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            Exception ex = new RuntimeException(Integer.toString(i));
            for (int j = 0; j < depth; j++) {
                ex = new RuntimeException(ex);
            }
            sum += LoggingFunctions.getStackTraceString(ex).hashCode();
        }
        final long end = System.nanoTime();
        LOG.log(Level.SEVERE, this.getClass().getName() + " 繰り返し:" + loop + " スタックの深さ:" + depth + " 秒数:" + ((end - start) / 1_000_000_000.0) + " チェックサム:" + sum);
    }

}
