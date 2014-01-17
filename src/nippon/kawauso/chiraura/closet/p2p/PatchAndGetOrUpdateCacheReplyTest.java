/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class PatchAndGetOrUpdateCacheReplyTest extends UsingRegistryTest<PatchAndGetOrUpdateCacheReply<?>> {

    @Override
    protected PatchAndGetOrUpdateCacheReply<?>[] getInstances() {
        int seed = 0;
        final GrowingBytes chunk = GrowingBytesTest.newInstance(seed++);
        return new PatchAndGetOrUpdateCacheReply<?>[] {
                PatchAndGetOrUpdateCacheReply.newRejected(),
                PatchAndGetOrUpdateCacheReply.newGiveUp(),
                PatchAndGetOrUpdateCacheReply.newNotFound(this.idRegistry, GrowingBytesTest.newId(seed++), seed++),
                new PatchAndGetOrUpdateCacheReply<>(true, this.chunkRegistry, GrowingBytesTest.newInstance(seed++), seed++),
                new PatchAndGetOrUpdateCacheReply<>(false, this.chunkRegistry, GrowingBytesTest.newInstance(seed++), seed++),
                new PatchAndGetOrUpdateCacheReply<>(true, this.chunkRegistry, chunk, chunk.getDate(), seed++),
                new PatchAndGetOrUpdateCacheReply<>(false, this.chunkRegistry, chunk, chunk.getDate(), seed++),
                new PatchAndGetOrUpdateCacheReply<>(true, this.chunkRegistry, chunk, chunk.getFirstDate(), seed++),
                new PatchAndGetOrUpdateCacheReply<>(false, this.chunkRegistry, chunk, chunk.getFirstDate(), seed++),
        };
    }

    @Override
    protected PatchAndGetOrUpdateCacheReply<?> getInstance(final int seed) {
        return new PatchAndGetOrUpdateCacheReply<>(true, this.chunkRegistry, GrowingBytesTest.newInstance(seed), seed);
    }

    @Override
    protected BytesConvertible.Parser<PatchAndGetOrUpdateCacheReply<?>> getParser() {
        return PatchAndGetOrUpdateCacheReply.getParser(this.chunkRegistry, this.idRegistry, this.diffRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
