/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.lib.test.TestFunctions;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public class BasicConstantTrafficLimiterTest {

    private static final Logger LOG = Logger.getLogger(BasicConstantTrafficLimiterTest.class.getName());

    /**
     * 初期化
     */
    public BasicConstantTrafficLimiterTest() {
        TestFunctions.testLogging(this.getClass().getName());
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

    private static void something(final long duration, final long sizeLimit, final int countLimit, final int numOfThreads, final int numOfAddresses,
            final int numOfLoops, final long penalty) throws Exception {
        final TrafficLimiter controller = new BasicConstantTrafficLimiter(duration, sizeLimit, countLimit, penalty);

        // テストデータを用意。
        final InetSocketAddress[] addresses = new InetSocketAddress[numOfAddresses];
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) i, (byte) (i + 1), (byte) (i + 2), (byte) (i + 3) }), i + 4);
        }

        final int[] sleepCount = new int[numOfThreads];
        final int[] throughCount = new int[numOfThreads];

        final List<Callable<Void>> threads = new ArrayList<>(numOfThreads);
        for (int i = 0; i < numOfThreads; i++) {
            final int id = i;

            threads.add(new Reporter<Void>(Level.SEVERE) {
                @Override
                public Void subCall() throws InterruptedException {
                    for (int j = 0; j < numOfLoops; j++) {
                        final long sleepTime;
                        if (j % 2 == 0) {
                            sleepTime = controller.nextSleep(sizeLimit / 10, addresses[id % addresses.length]);
                        } else {
                            sleepTime = controller.nextSleep(addresses[id % addresses.length]);
                        }
                        if (sleepTime > 0) {
                            sleepCount[id]++;
                            Thread.sleep(sleepTime);
                        } else {
                            throughCount[id]++;
                        }
                    }
                    return null;
                }
            });
        }

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final long start = System.nanoTime();
        final List<Future<Void>> futures = executor.invokeAll(threads);
        final long end = System.nanoTime();
        // 終処理。
        executor.shutdown();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));

        // エラーが起きていないか調べる。
        for (final Future<Void> future : futures) {
            future.get();
        }

        int sleepSum = 0;
        int throughSum = 0;
        for (int i = 0; i < sleepCount.length; i++) {
            sleepSum += sleepCount[i];
            throughSum += throughCount[i];
        }
        Assert.assertEquals(numOfThreads * numOfLoops, sleepSum + throughSum);

        LOG.log(Level.SEVERE, BasicSendQueuePoolTest.class.getName() + " プロセス数:" + numOfThreads + " 接続先数:" + numOfAddresses + " 繰り返し回数:" + numOfLoops + " 秒数:"
                + ((end - start) / 1_000_000_000.0) + " 単位消費ミリ秒数:" + ((end - start) / 1_000_000.0 / (numOfThreads * numOfLoops)) + " 睡眠数:" + sleepSum
                + " 素通り数:" + throughSum);

        // 破棄できるか検査。
        for (final InetSocketAddress address : addresses) {
            controller.remove(address);
            Assert.assertEquals(0, controller.nextSleep(address));
        }
    }

}
