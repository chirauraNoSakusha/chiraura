/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class PatchAndGetOrUpdateCacheMessageTest extends UsingRegistryTest<PatchAndGetOrUpdateCacheMessage<?>> {

    @Override
    protected PatchAndGetOrUpdateCacheMessage<?>[] getInstances() {
        int seed = 0;
        return new PatchAndGetOrUpdateCacheMessage[] {
                new PatchAndGetOrUpdateCacheMessage<>(this.idRegistry, GrowingBytesTest.newId(seed++), GrowingBytesEntryTest.newDiff(seed++)),
                new PatchAndGetOrUpdateCacheMessage<>(this.idRegistry, GrowingBytesTest.newId(seed++), GrowingBytesEntryTest.newDiff(seed++), seed++),
        };
    }

    @Override
    protected PatchAndGetOrUpdateCacheMessage<?> getInstance(final int seed) {
        return new PatchAndGetOrUpdateCacheMessage<>(this.idRegistry, GrowingBytesTest.newId(seed), GrowingBytesEntryTest.newDiff(seed), seed);
    }

    @Override
    protected BytesConvertible.Parser<PatchAndGetOrUpdateCacheMessage<?>> getParser() {
        return PatchAndGetOrUpdateCacheMessage.getParser(this.idRegistry, this.diffRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
