package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.storage.ConstantChunkTest;
import nippon.kawauso.chiraura.storage.VariableChunkTest;

/**
 * @author chirauraNoSakusha
 */
public final class GetCacheReplyTest extends UsingRegistryTest<GetCacheReply> {

    @Override
    protected GetCacheReply[] getInstances() {
        int seed = 0;
        return new GetCacheReply[] {
                GetCacheReply.newRejected(),
                GetCacheReply.newGiveUp(),
                GetCacheReply.newNotFound(this.idRegistry, ConstantChunkTest.newId(seed++), seed++),
                GetCacheReply.newNotFound(this.idRegistry, VariableChunkTest.newId(seed++), seed++),
                GetCacheReply.newNotFound(this.idRegistry, GrowingBytesTest.newId(seed++), seed++),
                new GetCacheReply(this.chunkRegistry, ConstantChunkTest.newInstance(seed++), seed++),
                new GetCacheReply(this.chunkRegistry, VariableChunkTest.newInstance(seed++), seed++),
                new GetCacheReply(this.chunkRegistry, GrowingBytesTest.newInstance(seed++), seed++),
        };
    }

    @Override
    protected GetCacheReply getInstance(final int seed) {
        return new GetCacheReply(this.chunkRegistry, GrowingBytesTest.newInstance(seed), seed);
    }

    @Override
    protected BytesConvertible.Parser<GetCacheReply> getParser() {
        return GetCacheReply.getParser(this.chunkRegistry, this.idRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
