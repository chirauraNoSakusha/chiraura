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
final class CcShortcutTableTest {

    static void testRandom(final CcShortcutTable instance, final int numOfPeers, final int loop) throws Exception {
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
            if (flag < 1 / 3.0) {
                instance.add(distances.get(random.nextInt(distances.size())));
            } else if (flag < 2 / 3.0) {
                instance.remove(distances.get(random.nextInt(distances.size())));
            } else {
                final Address target = AddressTest.newRandomInstance(random);
                final Address destination = instance.getRoutingDestination(target);
                if (instance.isEmpty()) {
                    Assert.assertNull(destination);
                } else {
                    if (destination == null) {
                        for (final Address distance : instance.getAll()) {
                            Assert.assertTrue(target.compareTo(distance) < 0);
                        }
                    }
                }
            }
        }
    }

    static void testRandom(final CcShortcutTable instance1, final CcShortcutTable instance2, final int numOfPeers, final int loop) throws Exception {
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
            if (flag < 1 / 3.0) {
                final Address peer = distances.get(random.nextInt(distances.size()));
                // System.out.println("ADD " + CcFunctions.distanceLevel(peer) + ":" + peer);
                Assert.assertEquals(instance1.add(peer), instance2.add(peer));
            } else if (flag < 2 / 3.0) {
                final Address peer = distances.get(random.nextInt(distances.size()));
                // System.out.println("REMOVE " + CcFunctions.distanceLevel(peer) + ":" + peer);
                Assert.assertEquals(instance1.remove(peer), instance2.remove(peer));
            } else {
                final Address distance = AddressTest.newRandomInstance(random);
                // System.out.println("GET " + CcFunctions.distanceLevel(distance) + ":" + distance);
                Assert.assertEquals(instance1.getRoutingDestination(distance), instance2.getRoutingDestination(distance));
            }
            // System.out.println("AFTER  1 " + instance1);
            // System.out.println("AFTER  2 " + instance2);
        }
    }

}
