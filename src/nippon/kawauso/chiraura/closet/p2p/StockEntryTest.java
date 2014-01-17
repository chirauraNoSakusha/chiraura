/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.base.HashValueTest;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.storage.Chunk;
import nippon.kawauso.chiraura.storage.ConstantChunkTest;
import nippon.kawauso.chiraura.storage.VariableChunkTest;

/**
 * @author chirauraNoSakusha
 */
public final class StockEntryTest extends UsingRegistryTest<StockEntry> {

    @Override
    protected StockEntry[] getInstances() {
        int seed = 0;
        return new StockEntry[] {
                new StockEntry(this.idRegistry, ConstantChunkTest.newId(seed++), seed++, HashValueTest.newInstance(seed++)),
                new StockEntry(this.idRegistry, VariableChunkTest.newId(seed++), seed++, HashValueTest.newInstance(seed++)),
                new StockEntry(this.idRegistry, GrowingBytesTest.newId(seed++), seed++, HashValueTest.newInstance(seed++)),
        };
    }

    static StockEntry newInstance(final TypeRegistry<Chunk.Id<?>> idRegistry, final int seed) {
        return new StockEntry(idRegistry, GrowingBytesTest.newId(seed), seed, HashValueTest.newInstance(seed));
    }

    @Override
    protected StockEntry getInstance(final int seed) {
        return newInstance(this.idRegistry, seed);
    }

    @Override
    protected BytesConvertible.Parser<StockEntry> getParser() {
        return StockEntry.getParser(this.idRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
