/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class UpdateChunkMessageTest extends UsingRegistryTest<UpdateChunkMessage> {

    @Override
    protected UpdateChunkMessage[] getInstances() {
        int seed = 0;
        return new UpdateChunkMessage[] {
                new UpdateChunkMessage(this.idRegistry, GrowingBytesTest.newId(seed++), seed++),
                new UpdateChunkMessage(this.idRegistry, GrowingBytesTest.newId(seed++), seed++),
                new UpdateChunkMessage(this.idRegistry, GrowingBytesTest.newId(seed++), seed++),
        };
    }

    @Override
    protected UpdateChunkMessage getInstance(final int seed) {
        return new UpdateChunkMessage(this.idRegistry, GrowingBytesTest.newId(seed), seed);
    }

    @Override
    protected BytesConvertible.Parser<UpdateChunkMessage> getParser() {
        return UpdateChunkMessage.getParser(this.idRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
