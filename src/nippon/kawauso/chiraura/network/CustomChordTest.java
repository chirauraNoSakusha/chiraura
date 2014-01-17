/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.AddressTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class CustomChordTest {

    /**
     * 起動試験。
     * @throws Exception 異常
     */
    @Test
    public void testBoot() throws Exception {
        final int capacity = 100;
        final long maintenanceInterval = 1_000L;
        final Random random = new Random();
        final Address self = AddressTest.newRandomInstance(random);
        final CustomChord instance = new CustomChord(self, capacity, maintenanceInterval);
        final ExecutorService executor = Executors.newCachedThreadPool();

        instance.start(executor);
        Thread.sleep(100);

        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }

}
