/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
public final class CcBossTest {

    private static InetSocketAddress randomPeer(final Random random) {
        final byte[] buff = new byte[4];
        random.nextBytes(buff);
        try {
            return new InetSocketAddress(InetAddress.getByAddress(buff), random.nextInt(1 << 16));
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testSample() throws Exception {
        final long interval = 500L;
        final int activeCapacity = 100;
        final Random random = new Random();
        final Address base = AddressTest.newRandomInstance(random);
        final CcView view = new BasicCcView(base, activeCapacity);
        final BlockingQueue<NetworkTask> taskQueue = new LinkedBlockingQueue<>();
        final ExecutorService executor = Executors.newCachedThreadPool();
        final CcBoss instance = new CcBoss(executor, view, interval, taskQueue);

        final int numOfPeers = 1 << 10;
        final Map<Address, InetSocketAddress> peers = new HashMap<>();
        for (int i = 0; i < numOfPeers; i++) {
            final Address address = base.addReverseBits(i);
            final InetSocketAddress peer = randomPeer(random);
            peers.put(address, peer);
        }

        executor.submit(instance);

        // 何も要請されないことの検査。
        Thread.sleep((long) (1.05 * interval));
        Assert.assertNull(taskQueue.poll());

        final Set<InetSocketAddress> socketAddresses = new HashSet<>();
        for (final Map.Entry<Address, InetSocketAddress> entry : peers.entrySet()) {
            view.addPeer(new AddressedPeer(entry.getKey(), entry.getValue()));
            socketAddresses.add(entry.getValue());
        }

        // 生存個体が居るなら、接触要請が出されることの検査。
        NetworkTask task = taskQueue.take();
        if (task instanceof AddressAccessRequest) {
            // 論理位置への接触要請。
            final BigInteger distance = view.getBase().distanceTo(((AddressAccessRequest) task).getAddress()).toBigInteger();
            Assert.assertTrue(distance.bitCount() == 1 || distance.bitCount() == Address.SIZE);
            // 生存個体への接触要請。
            task = taskQueue.take();
            Assert.assertTrue(task instanceof PeerAccessRequest);
            Assert.assertTrue(socketAddresses.contains(((PeerAccessRequest) task).getPeer()));
        } else {
            // 生存個体への接触要請。
            Assert.assertTrue(task instanceof PeerAccessRequest);
            Assert.assertTrue(socketAddresses.contains(((PeerAccessRequest) task).getPeer()));
            // 論理位置への接触要請。
            task = taskQueue.take();
            Assert.assertTrue(task instanceof AddressAccessRequest);
            final BigInteger distance = view.getBase().distanceTo(((AddressAccessRequest) task).getAddress()).toBigInteger();
            Assert.assertTrue(distance.bitCount() == 1 || distance.bitCount() == Address.SIZE);
        }

        // 生存個体が居ないなら、何も要請されないことの検査。
        for (final InetSocketAddress peer : peers.values()) {
            view.removePeer(peer);
        }

        Thread.sleep((long) (1.05 * interval));
        Assert.assertNull(taskQueue.poll());

        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
    }

}
