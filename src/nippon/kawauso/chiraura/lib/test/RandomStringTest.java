package nippon.kawauso.chiraura.lib.test;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class RandomStringTest {

    private static final Logger LOG = Logger.getLogger(RandomStringTest.class.getName());

    /**
     * 初期化
     */
    public RandomStringTest() {
        TestFunctions.testLogging(this.getClass().getName());
    }

    /**
     * 性能検査。
     */
    @Test
    public void test1() {
        LOG.log(Level.SEVERE, this.getClass().getName() + " 生成性能検査");
        final int loop = 100_000;
        final int length = 100;
        final long seed = System.currentTimeMillis();
        final RandomString random = new RandomString(seed);
        long sum = 0;
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            sum += random.nextString(length).hashCode();
        }
        final long end = System.nanoTime();
        LOG.log(Level.SEVERE, this.getClass().getName() + "繰り返し:" + loop + " 長さ:" + length + " 秒数:" + ((end - start) / 1_000_000_000.0) + " チェックサム:" + sum);
    }

}
