/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.ArrayList;
import java.util.Arrays;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class CheckStockReplyTest extends UsingRegistryTest<CheckStockReply> {

    @Override
    protected CheckStockReply[] getInstances() {
        int seed = 0;
        return new CheckStockReply[] {
                CheckStockReply.newRejected(),
                CheckStockReply.newGiveUp(),
                new CheckStockReply(new ArrayList<StockEntry>(0)),
                new CheckStockReply(Arrays.asList(StockEntryTest.newInstance(this.idRegistry, seed++), StockEntryTest.newInstance(this.idRegistry, seed++),
                        StockEntryTest.newInstance(this.idRegistry, seed++))),
        };
    }

    @Override
    protected CheckStockReply getInstance(final int seed) {
        return new CheckStockReply(Arrays.asList(StockEntryTest.newInstance(this.idRegistry, seed), StockEntryTest.newInstance(this.idRegistry, seed + 1),
                StockEntryTest.newInstance(this.idRegistry, seed + 2)));
    }

    @Override
    protected BytesConvertible.Parser<CheckStockReply> getParser() {
        return CheckStockReply.getParser(this.idRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
