package nippon.kawauso.chiraura.closet.p2p;

import java.util.Arrays;

import nippon.kawauso.chiraura.lib.base.AddressTest;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class CheckDemandMessageTest extends UsingRegistryTest<CheckDemandMessage> {

    @Override
    protected CheckDemandMessage[] getInstances() {
        int seed = 0;
        return new CheckDemandMessage[] {
                new CheckDemandMessage(AddressTest.newInstance(seed++), AddressTest.newInstance(seed++), Arrays.asList(
                        StockEntryTest.newInstance(this.idRegistry, seed++), StockEntryTest.newInstance(this.idRegistry, seed++),
                        StockEntryTest.newInstance(this.idRegistry, seed++))),
                new CheckDemandMessage(AddressTest.newInstance(seed++), AddressTest.newInstance(seed++), Arrays.asList(
                        StockEntryTest.newInstance(this.idRegistry, seed++), StockEntryTest.newInstance(this.idRegistry, seed++),
                        StockEntryTest.newInstance(this.idRegistry, seed++))),
                new CheckDemandMessage(AddressTest.newInstance(seed++), AddressTest.newInstance(seed++), Arrays.asList(
                        StockEntryTest.newInstance(this.idRegistry, seed++), StockEntryTest.newInstance(this.idRegistry, seed++),
                        StockEntryTest.newInstance(this.idRegistry, seed++))),
        };
    }

    @Override
    protected CheckDemandMessage getInstance(final int seed) {
        return new CheckDemandMessage(AddressTest.newInstance(seed), AddressTest.newInstance(seed), Arrays.asList(
                StockEntryTest.newInstance(this.idRegistry, seed), StockEntryTest.newInstance(this.idRegistry, seed + 1),
                StockEntryTest.newInstance(this.idRegistry, seed + 2)));
    }

    @Override
    protected BytesConvertible.Parser<CheckDemandMessage> getParser() {
        return CheckDemandMessage.getParser(this.idRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
