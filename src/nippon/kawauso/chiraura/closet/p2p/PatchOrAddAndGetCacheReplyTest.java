package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class PatchOrAddAndGetCacheReplyTest extends UsingRegistryTest<PatchOrAddAndGetCacheReply> {

    @Override
    protected PatchOrAddAndGetCacheReply[] getInstances() {
        int seed = 0;
        return new PatchOrAddAndGetCacheReply[] {
                PatchOrAddAndGetCacheReply.newRejected(),
                PatchOrAddAndGetCacheReply.newGiveUp(),
                new PatchOrAddAndGetCacheReply(this.chunkRegistry, GrowingBytesTest.newInstance(seed++), seed++),
        };
    }

    @Override
    protected PatchOrAddAndGetCacheReply getInstance(final int seed) {
        return new PatchOrAddAndGetCacheReply(this.chunkRegistry, GrowingBytesTest.newInstance(seed), seed);
    }

    @Override
    protected BytesConvertible.Parser<PatchOrAddAndGetCacheReply> getParser() {
        return PatchOrAddAndGetCacheReply.getParser(this.chunkRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
