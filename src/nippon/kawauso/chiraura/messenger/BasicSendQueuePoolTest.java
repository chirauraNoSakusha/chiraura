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
import java.util.concurrent.atomic.AtomicLong;
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
public final class BasicSendQueuePoolTest {

    private static final Logger LOG = Logger.getLogger(BasicSendQueuePoolTest.class.getName());

    /**
     * 初期化
     */
    public BasicSendQueuePoolTest() {
        TestFunctions.testLogging(this.getClass().getName());
    }

    /**
     * @throws Exception エラー
     */
    @Test
    public void testSample() throws Exception {
        final int numOfPeers = 100;
        final int numOfConnectionTypes = 2;
        final int numOfLoops = 1_000_000;

        // テストデータを用意。
        final InetSocketAddress[] peers = new InetSocketAddress[numOfLoops];
        final int[] connectionTypes = new int[peers.length];
        final List<List<Message>> mails = new ArrayList<>(peers.length);
        final InetAddress address = InetAddress.getLocalHost();
        final SendQueuePool queuePool = new BasicSendQueuePool();
        for (int i = 0; i < peers.length; i++) {
            final List<Message> mail = new ArrayList<>(1);
            mail.add(new TestMessage((int) (Math.random() * Long.MAX_VALUE)));
            mails.add(mail);
            peers[i] = new InetSocketAddress(address, (int) (Math.random() * numOfPeers));
            connectionTypes[i] = (int) (Math.random() * numOfConnectionTypes);
        }

        // 取り出し側。
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<Void> taker = executor.submit(new Reporter<Void>(Level.SEVERE) {
            @Override
            public Void subCall() throws InterruptedException {
                for (int i = 0; i < peers.length; i++) {
                    queuePool.addQueue(peers[i], connectionTypes[i], 0);
                    final List<Message> mail = queuePool.take(peers[i], connectionTypes[i]);
                    Assert.assertEquals(mails.get(i), mail);
                }
                return null;
            }
        });

        // 突っ込む側。
        for (int i = 0; i < peers.length; i++) {
            queuePool.put(peers[i], connectionTypes[i], mails.get(i));
        }

        // 終処理。
        executor.shutdown();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));

        // エラーが起きていないか調べる。
        taker.get();
    }

    /**
     * @throws Exception エラー
     */
    @Test
    public void testPerformance() throws Exception {
        final int numOfProcesses = 1_000;
        final int numOfPeers = 100;
        final int numOfConnectionTypes = 2;
        final int numOfLoops = 10_000;

        final long correctResult = numOfProcesses * (numOfLoops - 1L) * numOfLoops / 2L;

        // テストデータを用意。
        final InetSocketAddress[] peers = new InetSocketAddress[numOfLoops];
        final int[] connectionTypes = new int[peers.length];
        final InetAddress address = InetAddress.getLocalHost();
        for (int i = 0; i < peers.length; i++) {
            peers[i] = new InetSocketAddress(address, i / numOfConnectionTypes);
            connectionTypes[i] = i % numOfConnectionTypes;
        }
        final List<List<Message>> mails = new ArrayList<>(numOfLoops);
        for (int i = 0; i < numOfLoops; i++) {
            final List<Message> mail = new ArrayList<>(1);
            mail.add(new TestMessage(i));
            mails.add(mail);
        }

        final SendQueuePool queuePool = new BasicSendQueuePool();
        final AtomicLong result = new AtomicLong(0);
        final List<Callable<Void>> processes = new ArrayList<>(numOfProcesses * 2);
        for (int i = 0; i < numOfProcesses; i++) {
            final int connectionId = i;
            final int peerIndex = (int) (Math.random() * numOfPeers);

            // 取り出し側。
            processes.add(new Reporter<Void>(Level.SEVERE) {
                @Override
                public Void subCall() throws InterruptedException {
                    queuePool.addQueue(peers[peerIndex], connectionTypes[peerIndex], connectionId);
                    long sum = 0;
                    for (int j = 0; j < mails.size(); j++) {
                        final List<Message> mail = queuePool.take(peers[peerIndex], connectionTypes[peerIndex]);
                        sum += ((TestMessage) mail.get(0)).getValue();
                    }
                    result.addAndGet(sum);
                    return null;
                }
            });

            // 突っ込む側。
            processes.add(new Reporter<Void>(Level.SEVERE) {
                @Override
                public Void subCall() throws InterruptedException {
                    for (int j = 0; j < mails.size(); j++) {
                        queuePool.put(peers[peerIndex], connectionTypes[peerIndex], mails.get(j));
                    }
                    return null;
                }
            });
        }

        final ExecutorService executor = Executors.newFixedThreadPool(numOfProcesses * 2);
        final long start = System.nanoTime();
        final List<Future<Void>> futures = executor.invokeAll(processes);
        final long end = System.nanoTime();

        // 終処理。
        executor.shutdown();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));

        // エラーが起きていないか調べる。
        for (final Future<Void> future : futures) {
            future.get();
        }

        LOG.log(Level.SEVERE, BasicSendQueuePoolTest.class.getName() + " プロセス数:" + numOfProcesses + " 接続先数:" + numOfPeers + " 接続種別数:" + numOfConnectionTypes
                + " 繰り返し回数:" + numOfLoops + " 秒数:" + ((end - start) / 1_000_000_000.0) + " チェックサム:" + result.get());

        Assert.assertEquals(correctResult, result.get());
    }

}
