package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.storage.ConstantChunkTest;
import nippon.kawauso.chiraura.storage.VariableChunkTest;

/**
 * @author chirauraNoSakusha
 */
public final class AddCacheMessageTest extends UsingRegistryTest<AddCacheMessage> {

    @Override
    protected AddCacheMessage[] getInstances() {
        int seed = 0;
        return new AddCacheMessage[] {
                new AddCacheMessage(this.chunkRegistry, ConstantChunkTest.newInstance(seed++)),
                new AddCacheMessage(this.chunkRegistry, VariableChunkTest.newInstance(seed++)),
                new AddCacheMessage(this.chunkRegistry, GrowingBytesTest.newInstance(seed++)),
        };
    }

    @Override
    protected AddCacheMessage getInstance(final int seed) {
        return new AddCacheMessage(this.chunkRegistry, GrowingBytesTest.newInstance(seed));
    }

    @Override
    protected BytesConvertible.Parser<AddCacheMessage> getParser() {
        return AddCacheMessage.getParser(this.chunkRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
