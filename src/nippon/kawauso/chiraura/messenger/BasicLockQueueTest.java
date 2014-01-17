/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import nippon.kawauso.chiraura.lib.process.Reporter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class BasicLockQueueTest {

    /**
     * 準備。
     */
    @Before
    public void setUp() {}

    /**
     * @throws Exception エラー
     */
    @Test
    public void testSample() throws Exception {
        final int numOfLoops = 1_000_000;

        // テストデータを用意。
        final int[] elements = new int[numOfLoops];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = (int) (Math.random() * Integer.MAX_VALUE);
        }

        // 取り出し側。
        final BasicLockQueue<Integer> queue = new BasicLockQueue<>(new LinkedList<Integer>());
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<Void> taker = executor.submit(new Reporter<Void>(Level.SEVERE) {
            @Override
            public Void subCall() throws InterruptedException {
                for (int i = 0; i < elements.length; i++) {
                    queue.lock();
                    try {
                        final Integer element = queue.take();
                        Assert.assertNotNull(element);
                        Assert.assertEquals(elements[i], (int) element);
                    } finally {
                        queue.unlock();
                    }
                }
                return null;
            }
        });

        // 突っ込む側。
        for (int i = 0; i < elements.length; i++) {
            queue.lock();
            try {
                queue.put(elements[i]);
            } finally {
                queue.unlock();
            }
        }

        // 終処理。
        executor.shutdown();
        Assert.assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));

        // エラーが起きていないか調べる。
        taker.get();
    }

}
