/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class GetOrUpdateCacheMessageTest extends UsingRegistryTest<GetOrUpdateCacheMessage> {

    @Override
    protected GetOrUpdateCacheMessage[] getInstances() {
        int seed = 0;
        return new GetOrUpdateCacheMessage[] {
                new GetOrUpdateCacheMessage(this.idRegistry, GrowingBytesTest.newId(seed++)),
                new GetOrUpdateCacheMessage(this.idRegistry, GrowingBytesTest.newId(seed++), seed++),
        };
    }

    @Override
    protected GetOrUpdateCacheMessage getInstance(final int seed) {
        return new GetOrUpdateCacheMessage(this.idRegistry, GrowingBytesTest.newId(seed), seed);
    }

    @Override
    protected BytesConvertible.Parser<GetOrUpdateCacheMessage> getParser() {
        return GetOrUpdateCacheMessage.getParser(this.idRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
