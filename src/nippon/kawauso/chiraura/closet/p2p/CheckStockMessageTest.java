/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.ArrayList;
import java.util.Arrays;

import nippon.kawauso.chiraura.lib.base.AddressTest;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class CheckStockMessageTest extends UsingRegistryTest<CheckStockMessage> {

    @Override
    protected CheckStockMessage[] getInstances() {
        int seed = 0;
        return new CheckStockMessage[] {
                new CheckStockMessage(AddressTest.newInstance(seed++), AddressTest.newInstance(seed++), new ArrayList<StockEntry>(0)),
                new CheckStockMessage(AddressTest.newInstance(seed++), AddressTest.newInstance(seed++), Arrays.asList(
                        StockEntryTest.newInstance(this.idRegistry, seed++), StockEntryTest.newInstance(this.idRegistry, seed++),
                        StockEntryTest.newInstance(this.idRegistry, seed++))),
        };
    }

    @Override
    protected CheckStockMessage getInstance(final int seed) {
        return new CheckStockMessage(AddressTest.newInstance(seed), AddressTest.newInstance(seed + 1), Arrays.asList(
                StockEntryTest.newInstance(this.idRegistry, seed + 2), StockEntryTest.newInstance(this.idRegistry, seed + 3),
                StockEntryTest.newInstance(this.idRegistry, seed + 4)));
    }

    @Override
    protected BytesConvertible.Parser<CheckStockMessage> getParser() {
        return CheckStockMessage.getParser(this.idRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
