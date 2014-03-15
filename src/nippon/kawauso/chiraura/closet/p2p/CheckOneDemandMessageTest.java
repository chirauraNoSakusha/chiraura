package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class CheckOneDemandMessageTest extends UsingRegistryTest<CheckOneDemandMessage> {

    @Override
    protected CheckOneDemandMessage[] getInstances() {
        int seed = 0;
        return new CheckOneDemandMessage[] {
                new CheckOneDemandMessage(StockEntryTest.newInstance(this.idRegistry, seed++)),
                new CheckOneDemandMessage(StockEntryTest.newInstance(this.idRegistry, seed++)),
                new CheckOneDemandMessage(StockEntryTest.newInstance(this.idRegistry, seed++)),
        };
    }

    @Override
    protected CheckOneDemandMessage getInstance(final int seed) {
        return new CheckOneDemandMessage(StockEntryTest.newInstance(this.idRegistry, seed));
    }

    @Override
    protected BytesConvertible.Parser<CheckOneDemandMessage> getParser() {
        return CheckOneDemandMessage.getParser(this.idRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
