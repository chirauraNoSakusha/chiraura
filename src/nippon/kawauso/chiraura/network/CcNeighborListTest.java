/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.AddressTest;

import org.junit.Assert;

/**
 * @author chirauraNoSakusha
 */
public final class CcNeighborListTest {

    static void testRandom(final CcNeighborList instance, final int numOfPeers, final int loop) throws Exception {
        final Random random = new Random();

        final List<Address> distances = new ArrayList<>();
        while (distances.size() < numOfPeers) {
            final Address distance = AddressTest.newRandomInstance(random);
            if (!distance.equals(Address.ZERO)) {
                distances.add(distance);
            }
        }

        for (int i = 0; i < loop; i++) {
            final double flag = random.nextDouble();
            // System.out.printf("%.3f%n", flag);
            if (flag < 1 / 4.0) {
                instance.add(distances.get(random.nextInt(distances.size())));
            } else if (flag < 2 / 4.0) {
                instance.remove(distances.get(random.nextInt(distances.size())));
            } else if (flag < 3 / 4.0) {
                final Address neighbor = instance.getNeighbor();
                if (instance.isEmpty()) {
                    Assert.assertNull(neighbor);
                } else {
                    Assert.assertNotNull(neighbor);
                }
            } else {
                final int maxHop = random.nextInt(numOfPeers);
                final List<Address> neighbors = instance.getNeighbors(maxHop);
                if (instance.isEmpty()) {
                    Assert.assertEquals(0, neighbors.size());
                } else {
                    Assert.assertTrue(neighbors.size() <= maxHop);
                }
            }
        }
    }

    static void testRandom(final CcNeighborList instance1, final CcNeighborList instance2, final int numOfPeers, final int loop) throws Exception {
        final Random random = new Random();

        final List<Address> distances = new ArrayList<>();
        while (distances.size() < numOfPeers) {
            final Address distance = AddressTest.newRandomInstance(random);
            if (!distance.equals(Address.ZERO)) {
                distances.add(distance);
            }
        }

        for (int i = 0; i < loop; i++) {
            final double flag = random.nextDouble();
            // System.out.println("BEFORE 1 " + instance1);
            // System.out.println("BEFORE 2 " + instance2);
            if (flag < 1 / 4.0) {
                final Address peer = distances.get(random.nextInt(distances.size()));
                // System.out.println("ADD " + peer);
                Assert.assertEquals(instance1.add(peer), instance2.add(peer));
            } else if (flag < 2 / 4.0) {
                final Address peer = distances.get(random.nextInt(distances.size()));
                // System.out.println("REMOVE " + peer);
                Assert.assertEquals(instance1.remove(peer), instance2.remove(peer));
            } else if (flag < 3 / 4.0) {
                Assert.assertEquals(instance1.getNeighbor(), instance2.getNeighbor());
            } else {
                final int maxHop = random.nextInt(numOfPeers);
                // System.out.println("NEIGHBORS " + maxHop);
                final List<Address> neighbors1 = instance1.getNeighbors(maxHop);
                final List<Address> neighbors2 = instance2.getNeighbors(maxHop);
                Assert.assertEquals(neighbors1.size(), neighbors2.size());
                for (int j = 0; j < neighbors1.size(); j++) {
                    Assert.assertEquals(neighbors1.get(j), neighbors2.get(j));
                }
            }
            // System.out.println("AFTER 1 " + instance1);
            // System.out.println("AFTER 2 " + instance2);
        }
    }

}
