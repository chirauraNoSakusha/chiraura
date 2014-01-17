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
public final class GetChunkMessageTest extends UsingRegistryTest<GetChunkMessage> {

    @Override
    protected GetChunkMessage[] getInstances() {
        int seed = 0;
        return new GetChunkMessage[] {
                new GetChunkMessage(this.idRegistry, ConstantChunkTest.newId(seed++)),
                new GetChunkMessage(this.idRegistry, VariableChunkTest.newId(seed++)),
                new GetChunkMessage(this.idRegistry, GrowingBytesTest.newId(seed++)),
        };
    }

    @Override
    protected GetChunkMessage getInstance(final int seed) {
        return new GetChunkMessage(this.idRegistry, GrowingBytesTest.newId(seed));
    }

    @Override
    protected BytesConvertible.Parser<GetChunkMessage> getParser() {
        return GetChunkMessage.getParser(this.idRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
