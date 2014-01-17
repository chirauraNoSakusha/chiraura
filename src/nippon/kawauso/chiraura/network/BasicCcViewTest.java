/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.AddressTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class BasicCcViewTest {

    /**
     * @throws Exception 異常
     */
    @Test
    public void testGrowing() throws Exception {
        final int capacity = 100_000;
        final int loop = 100_000;
        final Address base = AddressTest.newRandomInstance(new Random());
        CcViewTest.testGrowing(new NaiveCcView(base), new BasicCcView(base, capacity), loop);
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testShrinking() throws Exception {
        final int capacity = 100_000;
        final int loop = 100_000;
        final Address base = AddressTest.newRandomInstance(new Random());
        CcViewTest.testShrinking(new NaiveCcView(base), new BasicCcView(base, capacity), loop);
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testRandom() throws Exception {
        final int capacity = 100_000;
        final int loop = 100_000;
        final Address base = AddressTest.newRandomInstance(new Random());
        CcViewTest.testRandom(new NaiveCcView(base), new BasicCcView(base, capacity), loop);
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testRandom2() throws Exception {
        final int capacity = 100_000;
        final int loop = 100_000;
        final Address base = AddressTest.newRandomInstance(new Random());
        CcViewTest.testRandom(new BasicCcView(base, capacity), new BasicCcView(base, capacity), loop);
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testCapacity() throws Exception {
        final int capacity = 1_000;
        final Random random = new Random();
        final Address base = AddressTest.newRandomInstance(random);
        final CcView instance = new BasicCcView(base, capacity);
        final Set<InetSocketAddress> peers = new HashSet<>(2 * capacity);
        while (peers.size() < capacity) {
            final AddressedPeer peer = AddressedPeerTest.randomInstance(random);
            if (!base.equals(peer.getAddress())) {
                instance.addPeer(peer);
                peers.add(peer.getPeer());
            }
        }

        // 以下、反対側の個体を消すためにいろいろやる。
        final Address targetPoint = base.addPowerOfTwo(Address.SIZE - 1);
        final AddressedPeer target = instance.getRoutingDestination(targetPoint);

        // 容量分の新しい個体を追加する。
        final byte[] buff = new byte[4];
        Address previous = target.getAddress();
        for (int i = 0; i <= capacity; i++) {
            final InetSocketAddress peer;
            while (true) {
                random.nextBytes(buff);
                final InetSocketAddress newPeer = new InetSocketAddress(InetAddress.getByAddress(buff), random.nextInt(1 << 16));
                if (peers.add(newPeer)) {
                    peer = newPeer;
                    break;
                }
            }

            previous = previous.addPowerOfTwo(0);
            instance.addPeer(new AddressedPeer(previous, peer));
            peers.add(peer);
        }

        // 追加した分の個体を削除する。
        for (int i = 0; i <= capacity + 1; i++) {
            final AddressedPeer peer = instance.getRoutingDestination(targetPoint);
            if (peer == null) {
                // 手前を全部削除しちゃった。
                break;
            }
            Assert.assertFalse(target.equals(peer));
            Assert.assertFalse(target.getAddress().equals(peer.getAddress()));
            Assert.assertFalse(target.getPeer().equals(peer.getPeer()));
            instance.removePeer(peer.getPeer());
        }
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testPeerChange() throws Exception {
        final int capacity = 1_000;
        final Random random = new Random();
        final Address base = AddressTest.newRandomInstance(random);
        final CcView instance = new BasicCcView(base, capacity);
        final Set<InetSocketAddress> peers = new HashSet<>(2 * capacity);
        while (peers.size() < capacity) {
            final AddressedPeer peer = AddressedPeerTest.randomInstance(random);
            if (!base.equals(peer.getAddress())) {
                instance.addPeer(peer);
                peers.add(peer.getPeer());
            }
        }

        final Address targetPoint = base.addPowerOfTwo(Address.SIZE - 1);
        final AddressedPeer target = instance.getRoutingDestination(targetPoint);

        // 個体を変えてみる。
        InetSocketAddress peer;
        while (true) {
            final byte[] buff = new byte[4];
            random.nextBytes(buff);
            peer = new InetSocketAddress(InetAddress.getByAddress(buff), random.nextInt(1 << 16));
            if (!peer.equals(target.getPeer())) {
                break;
            }
        }
        AddressedPeer newPeer = new AddressedPeer(target.getAddress(), peer);
        instance.addPeer(newPeer);
        AddressedPeer newTarget = instance.getRoutingDestination(targetPoint);
        Assert.assertEquals(newPeer, newTarget);

        // 論理位置を変えてみる。
        newPeer = new AddressedPeer(target.getAddress().subtractOne(), peer);
        instance.addPeer(newPeer);
        newTarget = instance.getRoutingDestination(targetPoint);
        Assert.assertEquals(newPeer, newTarget);
    }

    /**
     * 削除検査。
     */
    @Test
    public void testRemove() {
        final int capacity = 10_000;
        final Address base = AddressTest.newRandomInstance(new Random());
        final CcView instance = new BasicCcView(base, capacity);
        final int loop = 100_000;
        CcViewTest.testRemove(instance, loop);
    }

    /**
     * 論理位置変更検査。
     */
    @Test
    public void testChangeAddress() {
        final int capacity = 10_000;
        final Address base = AddressTest.newRandomInstance(new Random());
        final CcView instance = new BasicCcView(base, capacity);
        final int numOfPeers = 1_000;
        final int loop = 10_000;
        CcViewTest.testChangeAddress(instance, numOfPeers, loop);
    }

    /**
     * 個体変更検査。
     */
    @Test
    public void testChangePeer() {
        final int capacity = 10_000;
        final Address base = AddressTest.newRandomInstance(new Random());
        final CcView instance = new BasicCcView(base, capacity);
        final int numOfPeers = 1_000;
        final int loop = 10_000;
        CcViewTest.testChangePeer(instance, numOfPeers, loop);
    }

}
