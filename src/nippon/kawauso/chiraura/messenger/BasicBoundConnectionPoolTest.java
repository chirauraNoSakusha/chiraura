/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class BasicBoundConnectionPoolTest {

    /**
     * 検査。
     * @throws Exception 異常
     */
    @Test
    public void test() throws Exception {
        final BoundConnectionPool<ContactingConnection> instance = new BasicBoundConnectionPool<>();

        final List<InetSocketAddress> destinations = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            destinations.add(new InetSocketAddress(InetAddress.getByAddress(new byte[] { 0, 0, 0, (byte) (i / 5) }), i % 5));
        }

        Assert.assertTrue(instance.isEmpty());

        instance.add(new ContactingConnection(0, destinations.get(0), 0));

        Assert.assertEquals(1, instance.getNumOfConnections(destinations.get(0)));

        instance.add(new ContactingConnection(1, destinations.get(0), 0));

        Assert.assertEquals(2, instance.getNumOfConnections(destinations.get(0)));

        instance.add(new ContactingConnection(2, destinations.get(5), 0));

        Assert.assertEquals(2, instance.getNumOfConnections(destinations.get(0)));
        Assert.assertEquals(1, instance.getNumOfConnections(destinations.get(5)));

        instance.add(new ContactingConnection(3, destinations.get(1), 0));

        Assert.assertEquals(2, instance.getNumOfConnections(destinations.get(0)));
        Assert.assertEquals(1, instance.getNumOfConnections(destinations.get(5)));
        Assert.assertEquals(1, instance.getNumOfConnections(destinations.get(1)));

        instance.remove(0);

        Assert.assertEquals(1, instance.getNumOfConnections(destinations.get(0)));
        Assert.assertEquals(1, instance.getNumOfConnections(destinations.get(5)));
        Assert.assertEquals(1, instance.getNumOfConnections(destinations.get(1)));

        instance.remove(3);

        Assert.assertEquals(1, instance.getNumOfConnections(destinations.get(0)));
        Assert.assertEquals(1, instance.getNumOfConnections(destinations.get(5)));
        Assert.assertEquals(0, instance.getNumOfConnections(destinations.get(1)));

        Assert.assertTrue(instance.contains(destinations.get(0)));
        Assert.assertTrue(instance.contains(destinations.get(5)));
        Assert.assertFalse(instance.contains(destinations.get(1)));
        Assert.assertFalse(instance.contains(destinations.get(2)));
        Assert.assertFalse(instance.contains(destinations.get(3)));
        Assert.assertFalse(instance.contains(destinations.get(4)));
        Assert.assertFalse(instance.contains(destinations.get(6)));
        Assert.assertFalse(instance.contains(destinations.get(7)));
        Assert.assertFalse(instance.contains(destinations.get(8)));
        Assert.assertFalse(instance.contains(destinations.get(9)));
    }
}
