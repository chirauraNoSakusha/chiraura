/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.AddressTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class CcFingerDiggerTest {

    /**
     * 一例による検査。
     * @throws Exception 異常
     */
    @Test
    public void testSample() throws Exception {
        final long interval = 100L;
        final int activeCapacity = 100;
        final Random random = new Random();
        final Address base = AddressTest.newRandomInstance(random);
        final CcView view = new BasicCcView(base, activeCapacity);
        final BlockingQueue<NetworkTask> taskQueue = new LinkedBlockingQueue<>();
        final CcFingerStabilizer instance = new CcFingerStabilizer(null, interval, view, taskQueue);
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(instance);

        // 何も要請されないことの検査。
        Thread.sleep((long) (1.5 * interval));
        Assert.assertTrue(taskQueue.isEmpty());

        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
    }

}
