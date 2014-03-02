/**
 * 
 */
package nippon.kawauso.chiraura.lib.connection;

import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class BasicConstantTrafficLimiterTest extends ConstantTrafficLimiterTest {

    @Override
    TrafficLimiter getConstantTrafficLimiter(final long duration, final long sizeLimit, final int countLimit, final long penalty) {
        return new BasicConstantTrafficLimiter(duration, sizeLimit, countLimit, penalty);
    }

    /**
     * 使えるか検査。
     * @throws Exception 異常
     */
    @Test
    public void testNextSleep() throws Exception {
        final long duration = 1L;
        final long sizeLimit = 10_000_000L;
        final int countLimit = 1_000;
        final long penalty = duration * 10;
        final int numOfThreads = 100;
        final int numOfAddresses = numOfThreads / 10;
        final int numOfLoops = 100;
        something(duration, sizeLimit, countLimit, numOfThreads, numOfAddresses, numOfLoops, penalty);
    }

    /**
     * 性能検査。
     * @throws Exception 異常
     */
    @Test
    public void testPerformance() throws Exception {
        final long duration = 1L;
        final long sizeLimit = 10_000_000L;
        final int countLimit = 1_000;
        final long penalty = 0;
        final int numOfThreads = 100;
        final int numOfAddresses = numOfThreads / 10;
        final int numOfLoops = 1_000;
        something(duration, sizeLimit, countLimit, numOfThreads, numOfAddresses, numOfLoops, penalty);
    }

}
