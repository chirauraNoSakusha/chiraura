package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class GetOrUpdateCacheReplyTest extends UsingRegistryTest<GetOrUpdateCacheReply<?>> {

    @Override
    protected GetOrUpdateCacheReply<?>[] getInstances() {
        int seed = 0;
        final GrowingBytes chunk = GrowingBytesTest.newInstance(seed++);
        return new GetOrUpdateCacheReply[] {
                GetOrUpdateCacheReply.newRejected(),
                GetOrUpdateCacheReply.newGiveUp(),
                GetOrUpdateCacheReply.newNotFound(this.idRegistry, GrowingBytesTest.newId(seed++), seed++),
                new GetOrUpdateCacheReply<>(this.chunkRegistry, GrowingBytesTest.newInstance(seed++), seed++),
                new GetOrUpdateCacheReply<>(this.chunkRegistry, chunk, chunk.getDate(), seed++),
                new GetOrUpdateCacheReply<>(this.chunkRegistry, chunk, chunk.getFirstDate(), seed++),
        };
    }

    @Override
    protected GetOrUpdateCacheReply<?> getInstance(final int seed) {
        final GrowingBytes chunk = GrowingBytesTest.newInstance(seed);
        return new GetOrUpdateCacheReply<>(this.chunkRegistry, chunk, chunk.getFirstDate(), seed);
    }

    @Override
    protected BytesConvertible.Parser<GetOrUpdateCacheReply<?>> getParser() {
        return GetOrUpdateCacheReply.getParser(this.chunkRegistry, this.idRegistry, this.diffRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
