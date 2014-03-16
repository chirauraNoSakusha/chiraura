package nippon.kawauso.chiraura.lib.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.lib.test.TestFunctions;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class LockPoolTest {

    private static final Logger LOG = Logger.getLogger(LockPoolTest.class.getName());

    /**
     * 初期化
     */
    public LockPoolTest() {
        TestFunctions.testLogging(this.getClass().getSimpleName());
    }

    /**
     * 性能検査。
     * @throws Exception 異常
     */
    @Test
    public void testLockPerformance() throws Exception {
        LOG.log(Level.SEVERE, this.getClass().getName() + " 性能検査");

        final int loop = 1_000;
        final int numOfProcesses = 100;
        final int numOfLockers = numOfProcesses / 10;

        final LockPool<Integer> locks = new LockPool<>();
        final int[] locker = new int[numOfLockers];

        final Collection<Callable<Void>> processes = new ArrayList<>();
        final long seed = System.nanoTime();
        for (int i = 0; i < numOfProcesses; i++) {
            final int index = i;
            processes.add(new Reporter<Void>(Level.SEVERE) {
                @Override
                public Void subCall() throws Exception {
                    final Random random = new Random(seed + index);
                    for (int j = 0; j < loop; j++) {
                        // System.out.println(index + " " + j);
                        final int key = random.nextInt(numOfLockers);
                        locks.lock(key);
                        try {
                            final int value = locker[key];
                            Thread.sleep(0, random.nextInt(2));
                            locker[key] = value + 1;
                        } finally {
                            locks.unlock(key);
                        }
                    }
                    return null;
                }
            });
        }

        final ExecutorService executor = Executors.newFixedThreadPool(numOfProcesses);
        final long start = System.nanoTime();
        final Collection<Future<Void>> futures = executor.invokeAll(processes);
        final long end = System.nanoTime();

        executor.shutdown();
        for (final Future<Void> future : futures) {
            future.get();
        }

        int sum = 0;
        for (final int value : locker) {
            sum += value;
        }

        Assert.assertTrue(locks.isEmpty());
        Assert.assertEquals(loop * numOfProcesses, sum);
        LOG.log(Level.SEVERE, this.getClass().getName() + " 繰り返し回数:" + loop + " プロセス数:" + numOfProcesses + " ロック数:" + numOfLockers + " 秒数:"
                + ((end - start) / 1_000_000_000.0) + " チェックサム:" + sum);

    }

    /**
     * 性能検査。
     * @throws Exception 異常
     */
    @Test
    public void testTryLockPerformance() throws Exception {
        LOG.log(Level.SEVERE, this.getClass().getName() + " 性能検査");

        final int loop = 1_000;
        final int numOfProcesses = 100;
        final int numOfLockers = numOfProcesses / 10;

        final LockPool<Integer> locks = new LockPool<>();
        final int[] locker = new int[numOfLockers];

        final Collection<Callable<Void>> processes = new ArrayList<>();
        final long seed = System.nanoTime();
        for (int i = 0; i < numOfProcesses; i++) {
            final int index = i;
            processes.add(new Reporter<Void>(Level.SEVERE) {
                @Override
                public Void subCall() throws Exception {
                    final Random random = new Random(seed + index);
                    for (int j = 0; j < loop; j++) {
                        // System.out.println(index + " " + j);
                        final int key = random.nextInt(numOfLockers);
                        while (!locks.tryLock(key)) {
                            Thread.sleep(0, 1);
                        }
                        try {
                            final int value = locker[key];
                            Thread.sleep(0, random.nextInt(2));
                            locker[key] = value + 1;
                        } finally {
                            locks.unlock(key);
                        }
                    }
                    return null;
                }
            });
        }

        final ExecutorService executor = Executors.newFixedThreadPool(numOfProcesses);
        final long start = System.nanoTime();
        final Collection<Future<Void>> futures = executor.invokeAll(processes);
        final long end = System.nanoTime();

        executor.shutdown();
        for (final Future<Void> future : futures) {
            future.get();
        }

        int sum = 0;
        for (final int value : locker) {
            sum += value;
        }

        Assert.assertTrue(locks.isEmpty());
        Assert.assertEquals(loop * numOfProcesses, sum);
        LOG.log(Level.SEVERE, this.getClass().getName() + " 繰り返し回数:" + loop + " プロセス数:" + numOfProcesses + " ロック数:" + numOfLockers + " 秒数:"
                + ((end - start) / 1_000_000_000.0) + " チェックサム:" + sum);

    }

}
