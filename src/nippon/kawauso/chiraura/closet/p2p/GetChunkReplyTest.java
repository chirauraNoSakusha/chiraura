/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.storage.ConstantChunkTest;
import nippon.kawauso.chiraura.storage.VariableChunkTest;

/**
 * @author chirauraNoSakusha
 */
public final class GetChunkReplyTest extends UsingRegistryTest<GetChunkReply> {

    @Override
    protected GetChunkReply[] getInstances() {
        int seed = 0;
        return new GetChunkReply[] {
                GetChunkReply.newRejected(),
                GetChunkReply.newGiveUp(),
                GetChunkReply.newNotFound(),
                new GetChunkReply(this.chunkRegistry, ConstantChunkTest.newInstance(seed++)),
                new GetChunkReply(this.chunkRegistry, VariableChunkTest.newInstance(seed++)),
                new GetChunkReply(this.chunkRegistry, GrowingBytesTest.newInstance(seed++)),
        };
    }

    @Override
    protected GetChunkReply getInstance(final int seed) {
        return new GetChunkReply(this.chunkRegistry, GrowingBytesTest.newInstance(seed));
    }

    @Override
    protected BytesConvertible.Parser<GetChunkReply> getParser() {
        return GetChunkReply.getParser(this.chunkRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
