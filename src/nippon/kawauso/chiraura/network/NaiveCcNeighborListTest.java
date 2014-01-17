/**
 * 
 */
package nippon.kawauso.chiraura.network;

import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class NaiveCcNeighborListTest {

    /**
     * @throws Exception 異常
     */
    @Test
    public void testRandom() throws Exception {
        final int numOfPeers = 10_000;
        final int loop = 100_000;
        CcNeighborListTest.testRandom(new NaiveCcNeighborList(), numOfPeers, loop);
    }

}
