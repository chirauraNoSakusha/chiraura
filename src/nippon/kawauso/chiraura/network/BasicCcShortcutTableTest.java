/**
 * 
 */
package nippon.kawauso.chiraura.network;

import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class BasicCcShortcutTableTest {

    /**
     * @throws Exception 異常
     */
    @Test
    public void testRandom() throws Exception {
        final int numOfPeers = 10_000;
        final int loop = 1_000_000;
        CcShortcutTableTest.testRandom(new BasicCcShortcutTable(), numOfPeers, loop);
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testControlRandom() throws Exception {
        final int numOfPeers = 10_000;
        final int loop = 1_000_000;
        CcShortcutTableTest.testRandom(new NaiveCcShortcutTable(), new BasicCcShortcutTable(), numOfPeers, loop);
    }

}
