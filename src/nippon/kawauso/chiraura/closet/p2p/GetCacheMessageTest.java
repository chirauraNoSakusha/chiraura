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
public final class GetCacheMessageTest extends UsingRegistryTest<GetCacheMessage> {

    @Override
    protected GetCacheMessage[] getInstances() {
        int seed = 0;
        return new GetCacheMessage[] {
                new GetCacheMessage(this.idRegistry, ConstantChunkTest.newId(seed++)),
                new GetCacheMessage(this.idRegistry, VariableChunkTest.newId(seed++)),
                new GetCacheMessage(this.idRegistry, GrowingBytesTest.newId(seed++)),
        };
    }

    @Override
    protected GetCacheMessage getInstance(final int seed) {
        return new GetCacheMessage(this.idRegistry, GrowingBytesTest.newId(seed));
    }

    @Override
    protected BytesConvertible.Parser<GetCacheMessage> getParser() {
        return GetCacheMessage.getParser(this.idRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
