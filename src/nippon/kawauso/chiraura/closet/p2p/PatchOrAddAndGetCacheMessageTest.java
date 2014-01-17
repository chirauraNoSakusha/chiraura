/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class PatchOrAddAndGetCacheMessageTest extends UsingRegistryTest<PatchOrAddAndGetCacheMessage> {

    @Override
    protected PatchOrAddAndGetCacheMessage[] getInstances() {
        int seed = 0;
        return new PatchOrAddAndGetCacheMessage[] {
                new PatchOrAddAndGetCacheMessage(this.chunkRegistry, GrowingBytesTest.newInstance(seed++)),
                new PatchOrAddAndGetCacheMessage(this.chunkRegistry, GrowingBytesTest.newInstance(seed++)),
                new PatchOrAddAndGetCacheMessage(this.chunkRegistry, GrowingBytesTest.newInstance(seed++)),
        };
    }

    @Override
    protected PatchOrAddAndGetCacheMessage getInstance(final int seed) {
        return new PatchOrAddAndGetCacheMessage(this.chunkRegistry, GrowingBytesTest.newInstance(seed));
    }

    @Override
    protected BytesConvertible.Parser<PatchOrAddAndGetCacheMessage> getParser() {
        return PatchOrAddAndGetCacheMessage.getParser(this.chunkRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

    @Override
    protected int getNumOfExceptionLoops() {
        return 10 * getNumOfLoops();
    }

}
