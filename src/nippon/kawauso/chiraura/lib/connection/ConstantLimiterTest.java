package nippon.kawauso.chiraura.lib.connection;

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
public final class ConstantLimiterTest {
    private static final Logger LOG = Logger.getLogger(ConstantLimiterTest.class.getName());

    /**
     * 初期化。
     */
    public ConstantLimiterTest() {
        TestFunctions.testLogging(this.getClass().getName());
    }

    /**
     * 使えるか検査。
     * @throws Exception 異常
     */
    @Test
    public void testNextSleep() throws Exception {
        final long duration = 1L;
        final long valueLimit = 10_000_000L;
        final int countLimit = 1_000;
        final long penalty = 10L;
        final long value = valueLimit / 10;
        final int numOfTargets = 10;
        final int numOfProcesses = 100;
        final int numOfLoops = 100;
        final ConstantLimiter<Integer> instance = new ConstantLimiter<Integer>(duration, valueLimit, countLimit, penalty) {};
        something(instance, value, numOfTargets, numOfProcesses, numOfLoops);
    }

    /**
     * 性能検査。
     * @throws Exception 異常
     */
    @Test
    public void testPerformance() throws Exception {
        final long duration = 1L;
        final long valueLimit = 10_000_000L;
        final int countLimit = 1_000;
        final long penalty = 0;
        final long value = valueLimit / 10;
        final int numOfProcesses = 100;
        final int numOfTargets = numOfProcesses / 10;
        final int numOfLoops = 1_000;
        final ConstantLimiter<Integer> instance = new ConstantLimiter<Integer>(duration, valueLimit, countLimit, penalty) {};
        something(instance, value, numOfTargets, numOfProcesses, numOfLoops);
    }

    void something(final Limiter<Integer> instance, final long value, final int numOfTargets, final int numOfProcesses, final int numOfLoops)
            throws Exception {
        final int[] sleepCount = new int[numOfProcesses];
        final int[] throughCount = new int[numOfProcesses];

        final List<Callable<Void>> threads = new ArrayList<>(numOfProcesses);
        for (int i = 0; i < numOfProcesses; i++) {
            final int id = i;

            threads.add(new Reporter<Void>(Level.SEVERE) {
                @Override
                public Void subCall() throws InterruptedException {
                    for (int j = 0; j < numOfLoops; j++) {
                        final long sleepTime;
                        if (j % 2 == 0) {
                            sleepTime = instance.addValueAndCheckPenalty(id % numOfTargets, value);
                        } else {
                            sleepTime = instance.checkPenalty(id % numOfTargets);
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
        Assert.assertEquals(numOfProcesses * numOfLoops, sleepSum + throughSum);

        LOG.log(Level.SEVERE, getClass().getName() + " プロセス数:" + numOfProcesses + " 制限対象数:" + numOfTargets + " 繰り返し回数:" + numOfLoops + " 秒数:"
                + ((end - start) / 1_000_000_000.0) + " 単位消費ミリ秒数:" + ((end - start) / 1_000_000.0 / (numOfProcesses * numOfLoops)) + " 睡眠数:" + sleepSum
                + " 素通り数:" + throughSum);
        // 破棄できるか検査。
        for (int i = 0; i < numOfTargets; i++) {
            instance.remove(i);
            Assert.assertEquals(0, instance.checkPenalty(i));
        }
    }

}
