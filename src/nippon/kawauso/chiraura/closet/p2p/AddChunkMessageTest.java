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
public final class AddChunkMessageTest extends UsingRegistryTest<AddChunkMessage> {

    @Override
    protected AddChunkMessage[] getInstances() {
        int seed = 0;
        return new AddChunkMessage[] {
                new AddChunkMessage(this.chunkRegistry, ConstantChunkTest.newInstance(seed++)),
                new AddChunkMessage(this.chunkRegistry, VariableChunkTest.newInstance(seed++)),
                new AddChunkMessage(this.chunkRegistry, GrowingBytesTest.newInstance(seed++)),
        };
    }

    @Override
    protected AddChunkMessage getInstance(final int seed) {
        return new AddChunkMessage(this.chunkRegistry, GrowingBytesTest.newInstance(seed));
    }

    @Override
    protected BytesConvertible.Parser<AddChunkMessage> getParser() {
        return AddChunkMessage.getParser(this.chunkRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
