package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.messenger.ReceivedMail;
import nippon.kawauso.chiraura.messenger.ReceivedMailTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class SessionManagerTest {

    private final Random random;
    private final SessionManager instance;

    /**
     * 初期化。
     */
    public SessionManagerTest() {
        this.random = new Random();
        this.instance = new SessionManager();
    }

    private InetSocketAddress randomDestination() {
        final byte[] addr = new byte[4];
        this.random.nextBytes(addr);
        try {
            return new InetSocketAddress(InetAddress.getByAddress(addr), this.random.nextInt(1 << 16));
        } catch (final UnknownHostException e) {
            // ここには来ないはず。
            throw new RuntimeException(e);
        }
    }

    private void testTimeout(final long timeout) throws InterruptedException {
        final InetSocketAddress destination = randomDestination();
        final Session session = this.instance.newSession(destination);
        final long start = System.nanoTime();
        final ReceivedMail reply = this.instance.waitReply(session, timeout);
        final long end = System.nanoTime();
        Assert.assertNull(reply);
        Assert.assertTrue(timeout <= (end - start) / 1_000_000);
    }

    /**
     * 時間切れ動作の検査。
     * @throws Exception 異常
     */
    @Test
    public void testTimeout() throws Exception {
        testTimeout(100);
        testTimeout(1);
        testTimeout(0);
        testTimeout(-1);
        testTimeout(-100);
    }

    /**
     * 自作自演の正常系。
     * @throws Exception 異常
     */
    @Test
    public void testAlone() throws Exception {
        final ReceivedMail reply = ReceivedMailTest.newRandomInstance(this.random);
        final Session session = this.instance.newSession(reply.getSourcePeer());

        Assert.assertTrue(this.instance.setReply(session, reply));

        final ReceivedMail reply2 = this.instance.waitReply(session, 100);

        Assert.assertEquals(reply, reply2);
    }

    /**
     * 一対一正常系。
     * @throws Exception 異常
     */
    @Test
    public void testOneToOne() throws Exception {
        final long timeout = 100;
        final ReceivedMail reply = ReceivedMailTest.newRandomInstance(this.random);
        final Session session = this.instance.newSession(reply.getSourcePeer());

        final List<Callable<Void>> workers = new ArrayList<>();
        workers.add(new Reporter<Void>(Level.SEVERE) {
            @Override
            public Void subCall() throws Exception {
                Thread.sleep(timeout / 2);
                Assert.assertTrue(SessionManagerTest.this.instance.setReply(session, reply));
                return null;
            }
        });
        workers.add(new Reporter<Void>(Level.SEVERE) {
            @Override
            public Void subCall() throws Exception {
                final ReceivedMail reply2 = SessionManagerTest.this.instance.waitReply(session, timeout);
                Assert.assertEquals(reply, reply2);
                return null;
            }
        });

        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final long start = System.nanoTime();
        final List<Future<Void>> futures = executor.invokeAll(workers);
        final long end = System.nanoTime();

        for (final Future<Void> future : futures) {
            future.get();
        }

        Assert.assertTrue(timeout / 2 <= (end - start) / 1_000_000);
        Assert.assertTrue((end - start) / 1_000_000 <= timeout);
    }
}
