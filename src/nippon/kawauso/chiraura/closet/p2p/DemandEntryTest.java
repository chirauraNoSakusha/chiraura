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
public final class DemandEntryTest extends UsingRegistryTest<DemandEntry> {

    @Override
    protected DemandEntry[] getInstances() {
        int seed = 0;
        return new DemandEntry[] {
                new DemandEntry(this.idRegistry, ConstantChunkTest.newId(seed++)),
                new DemandEntry(this.idRegistry, VariableChunkTest.newId(seed++)),
                new DemandEntry(this.idRegistry, GrowingBytesTest.newId(seed++)),
                new DemandEntry(this.idRegistry, ConstantChunkTest.newId(seed++), seed++, HashValueTest.newInstance(seed++)),
                new DemandEntry(this.idRegistry, VariableChunkTest.newId(seed++), seed++, HashValueTest.newInstance(seed++)),
                new DemandEntry(this.idRegistry, GrowingBytesTest.newId(seed++), seed++, HashValueTest.newInstance(seed++)),
        };
    }

    static DemandEntry newInstance(final TypeRegistry<Chunk.Id<?>> idRegistry, final int seed) {
        return new DemandEntry(idRegistry, GrowingBytesTest.newId(seed), seed, HashValueTest.newInstance(seed));
    }

    @Override
    protected DemandEntry getInstance(final int seed) {
        return newInstance(this.idRegistry, seed);
    }

    @Override
    protected BytesConvertible.Parser<DemandEntry> getParser() {
        return DemandEntry.getParser(this.idRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
