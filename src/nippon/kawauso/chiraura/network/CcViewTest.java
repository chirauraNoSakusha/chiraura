package nippon.kawauso.chiraura.network;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.AddressTest;
import nippon.kawauso.chiraura.lib.connection.PeerTest;

import org.junit.Assert;

/**
 * @author chirauraNoSakusha
 */
public final class CcViewTest {

    static void testGrowing(final CcView instance1, final CcView instance2, final int loop) throws Exception {
        final Random random = new Random();

        for (int i = 0; i < loop; i++) {
            // System.out.println("BEFORE 1 " + instance1);
            // System.out.println("BEFORE 2 " + instance2);

            final AddressedPeer peer = AddressedPeerTest.randomInstance(random);
            final Address address = AddressTest.newRandomInstance(random);

            // System.out.println("PEER " + peer);
            // System.out.println("ADDRESS " + address);

            // addPeer
            // System.out.println("BEFORE 1 " + instance1);
            // System.out.println("BEFORE 2 " + instance2);
            // System.out.println("ADD_PEER " + peer + " " + instance1.getBase().distanceTo(peer.getAddress()));
            Assert.assertEquals(instance1.addPeer(peer), instance2.addPeer(peer));

            // dominates
            // System.out.println("DOMINATES " + address);
            Assert.assertEquals(instance1.dominates(address), instance2.dominates(address));

            // getRoutingDestination
            // System.out.println("GET_ROUTING_DESTINATION " + address);
            Assert.assertEquals(instance1.getRoutingDestination(address), instance2.getRoutingDestination(address));

            // getSuccessors
            // System.out.println("BEFORE 1 " + instance1);
            // System.out.println("BEFORE 2 " + instance2);
            // System.out.println("GET_SUCCESSORS");
            Assert.assertEquals(instance1.getSuccessors(Integer.MAX_VALUE), instance2.getSuccessors(Integer.MAX_VALUE));

            // getPredecessors
            // System.out.println("BEFORE 1 " + instance1);
            // System.out.println("BEFORE 2 " + instance2);
            // System.out.println("GET_PredeCESSORS");
            Assert.assertEquals(instance1.getPredecessors(Integer.MAX_VALUE), instance2.getPredecessors(Integer.MAX_VALUE));

            // getFingers
            // System.out.println("GET_FINGERS");
            Assert.assertEquals(instance1.getFingers(), instance2.getFingers());

            // System.out.println("AFTER 1 " + instance1);
            // System.out.println("AFTER 2 " + instance2);
        }
    }

    static void testShrinking(final CcView instance1, final CcView instance2, final int loop) throws Exception {
        final Random random = new Random();

        final List<AddressedPeer> peers = new ArrayList<>();
        for (int i = 0; i < loop; i++) {
            final AddressedPeer peer = AddressedPeerTest.randomInstance(random);
            Assert.assertEquals(instance1.addPeer(peer), instance2.addPeer(peer));
            peers.add(peer);
        }

        for (int i = 0; i < loop; i++) {
            // System.out.println("BEFORE 1 " + instance1);
            // System.out.println("BEFORE 2 " + instance2);

            final int k = random.nextInt(peers.size());
            final AddressedPeer peer = peers.get(k);
            peers.set(k, peers.get(peers.size() - 1));
            peers.remove(peers.size() - 1);
            final Address address = AddressTest.newRandomInstance(random);

            // System.out.println("PEER " + peer);
            // System.out.println("ADDRESS " + address);

            // removePeer
            // System.out.println("BEFORE 1 " + instance1);
            // System.out.println("BEFORE 2 " + instance2);
            // System.out.println("REMOVE_PEER " + peer + " " + instance1.getBase().distanceTo(peer.getAddress()));
            Assert.assertEquals(instance1.removePeer(peer.getPeer()), instance2.removePeer(peer.getPeer()));
            // System.out.println("AFTER 1 " + instance1);
            // System.out.println("AFTER 2 " + instance2);

            // dominates
            // System.out.println("DOMINATES " + address);
            Assert.assertEquals(instance1.dominates(address), instance2.dominates(address));

            // getRoutingDestination
            // System.out.println("GET_ROUTING_DESTINATION " + address);
            Assert.assertEquals(instance1.getRoutingDestination(address), instance2.getRoutingDestination(address));

            // getSuccessors
            // System.out.println("GET_SUCCESSORS");
            Assert.assertEquals(instance1.getSuccessors(Integer.MAX_VALUE), instance2.getSuccessors(Integer.MAX_VALUE));

            // getPredecessors
            // System.out.println("GET_PREDECESSORS");
            Assert.assertEquals(instance1.getPredecessors(Integer.MAX_VALUE), instance2.getPredecessors(Integer.MAX_VALUE));

            // getFingers
            // System.out.println("GET_FINGERS");
            Assert.assertEquals(instance1.getFingers(), instance2.getFingers());

            // System.out.println("AFTER 1 " + instance1);
            // System.out.println("AFTER 2 " + instance2);
        }
    }

    static void testRandom(final CcView instance1, final CcView instance2, final int loop) throws Exception {
        final Random random = new Random();

        final List<AddressedPeer> peers = new ArrayList<>();
        for (int i = 0; i < loop; i++) {
            final AddressedPeer peer = AddressedPeerTest.randomInstance(random);
            peers.add(peer);
        }

        for (int i = 0; i < loop; i++) {
            // System.out.println("BEFORE 1 " + instance1);
            // System.out.println("BEFORE 2 " + instance2);

            final AddressedPeer peer = peers.get(random.nextInt(peers.size()));
            final Address address = AddressTest.newRandomInstance(random);

            // System.out.println("PEER " + peer);
            // System.out.println("ADDRESS " + address);

            if (random.nextDouble() < 1.0 / 2.0) {
                // addPeer
                // System.out.println("ADD_PEER " + peer);
                Assert.assertEquals(instance1.addPeer(peer), instance2.addPeer(peer));
            } else {
                // removePeer
                // System.out.println("REMOVE_PEER " + peer);
                Assert.assertEquals(instance1.removePeer(peer.getPeer()), instance2.removePeer(peer.getPeer()));
            }

            // dominates
            // System.out.println("DOMINATES " + address);
            Assert.assertEquals(instance1.dominates(address), instance2.dominates(address));

            // getRoutingDestination
            // System.out.println("GET_ROUTING_DESTINATION " + address);
            Assert.assertEquals(instance1.getRoutingDestination(address), instance2.getRoutingDestination(address));

            // getSuccessors
            // System.out.println("GET_SUCCESSORS");
            Assert.assertEquals(instance1.getSuccessors(Integer.MAX_VALUE), instance2.getSuccessors(Integer.MAX_VALUE));

            // getPredecessors
            // System.out.println("GET_PredeCESSORS");
            Assert.assertEquals(instance1.getPredecessors(Integer.MAX_VALUE), instance2.getPredecessors(Integer.MAX_VALUE));

            // getFingers
            // System.out.println("GET_FINGERS");
            Assert.assertEquals(instance1.getFingers(), instance2.getFingers());

            // System.out.println("AFTER 1 " + instance1);
            // System.out.println("AFTER 2 " + instance2);
        }
    }

    /**
     * 除去した個体は出てこないことの検査。
     */
    static void testRemove(final CcView instance, final int loop) {
        final Random random = new Random();

        final Set<AddressedPeer> peers = new HashSet<>();
        while (peers.size() < loop) {
            peers.add(AddressedPeerTest.randomInstance(random));
        }

        for (final AddressedPeer peer : peers) {
            instance.addPeer(peer);
        }

        for (final AddressedPeer peer : peers) {
            instance.removePeer(peer.getPeer());

            Assert.assertFalse((new HashSet<>(instance.getImportantPeers())).contains(peer));
        }
    }

    /**
     * 論理位置を変える検査。
     */
    static void testChangeAddress(final CcView instance, final int numOfPeers, final int loop) {
        final Random random = new Random();

        final Set<Address> usedAddress = new HashSet<>();
        final Map<InetSocketAddress, Address> peerToAddress = new HashMap<>();
        while (peerToAddress.size() < numOfPeers) {
            final AddressedPeer peer = AddressedPeerTest.randomInstance(random);
            if (usedAddress.add(peer.getAddress())) {
                peerToAddress.put(peer.getPeer(), peer.getAddress());
                instance.addPeer(peer);
            }
        }

        final List<InetSocketAddress> peers = new ArrayList<>(peerToAddress.keySet());
        for (int i = 0; i < loop; i++) {
            final InetSocketAddress peer = peers.get(random.nextInt(peers.size()));
            Address address;
            while (true) {
                address = AddressTest.newRandomInstance(random);
                if (usedAddress.add(address)) {
                    break;
                }
            }
            peerToAddress.put(peer, address);
            instance.addPeer(new AddressedPeer(address, peer));
        }

        for (final Map.Entry<InetSocketAddress, Address> entry : peerToAddress.entrySet()) {
            final Address address = instance.removePeer(entry.getKey());
            if (address != null) {
                Assert.assertEquals(entry.getValue(), address);
            }
        }

        Assert.assertEquals(new ArrayList<AddressedPeer>(0), instance.getPeers());
    }

    /**
     * IPとポートを変える検査。
     */
    static void testChangePeer(final CcView instance, final int numOfPeers, final int loop) {
        final Random random = new Random();

        final Map<Address, InetSocketAddress> addressToPeer = new HashMap<>();
        while (addressToPeer.size() < numOfPeers) {
            final AddressedPeer peer = AddressedPeerTest.randomInstance(random);
            addressToPeer.put(peer.getAddress(), peer.getPeer());
            instance.addPeer(peer);
        }

        final List<Address> addresses = new ArrayList<>(addressToPeer.keySet());
        for (int i = 0; i < loop; i++) {
            final Address address = addresses.get(random.nextInt(addresses.size()));
            final InetSocketAddress peer = PeerTest.newRandomInstance(random);
            addressToPeer.put(address, peer);
            instance.addPeer(new AddressedPeer(address, peer));
        }

        for (final Map.Entry<Address, InetSocketAddress> entry : addressToPeer.entrySet()) {
            final Address address = instance.removePeer(entry.getValue());
            if (address != null) {
                Assert.assertEquals(entry.getKey(), address);
            }
        }

        Assert.assertEquals(new ArrayList<AddressedPeer>(0), instance.getPeers());
    }
}
