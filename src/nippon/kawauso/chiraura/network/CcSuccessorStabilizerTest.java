/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.AddressTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class CcSuccessorStabilizerTest {

    /**
     * 一例による検査。
     * @throws Exception 異常
     */
    @Test
    public void testSample() throws Exception {
        final long interval = 100L;
        final int capacity = 100;
        final Random random = new Random();
        final Address base = AddressTest.newRandomInstance(random);
        final CcView view = new BasicCcView(base, capacity);
        final BlockingQueue<NetworkTask> taskQueue = new LinkedBlockingQueue<>();
        final CcSuccessorStabilizer instance = new CcSuccessorStabilizer(null, interval, view, taskQueue);
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(instance);

        // 何も要請されないことの検査。
        Thread.sleep((long) (1.5 * interval));
        Assert.assertTrue(taskQueue.isEmpty());

        // 生存個体が居るなら、生存個体への接触要請が出されることの検査。
        final int numOfPeers = 1000;
        final Set<AddressedPeer> addrPeers = new HashSet<>();
        final Set<InetSocketAddress> peers = new HashSet<>();
        for (int i = 0; i < numOfPeers; i++) {
            final AddressedPeer peer = AddressedPeerTest.randomInstance(random);
            view.addPeer(peer);
            addrPeers.add(peer);
            peers.add(peer.getPeer());
        }

        final NetworkTask task = taskQueue.take();
        Assert.assertTrue(task instanceof PeerAccessRequest);
        Assert.assertTrue(peers.contains(((PeerAccessRequest) task).getPeer()));

        // 生存個体が居ないなら、何も要請されないことの検査。
        for (final AddressedPeer peer : addrPeers) {
            view.removePeer(peer.getPeer());
        }

        Thread.sleep((long) (1.5 * interval));
        Assert.assertTrue(taskQueue.isEmpty());

        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
    }

}
