package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class PatchChunkMessageTest extends UsingRegistryTest<PatchChunkMessage<?>> {

    @Override
    protected PatchChunkMessage<?>[] getInstances() {
        int seed = 0;
        return new PatchChunkMessage[] {
                new PatchChunkMessage<>(this.idRegistry, GrowingBytesTest.newId(seed++), GrowingBytesEntryTest.newDiff(seed++)),
                new PatchChunkMessage<>(this.idRegistry, GrowingBytesTest.newId(seed++), GrowingBytesEntryTest.newDiff(seed++)),
        };
    }

    @Override
    protected PatchChunkMessage<?> getInstance(final int seed) {
        return new PatchChunkMessage<>(this.idRegistry, GrowingBytesTest.newId(seed), GrowingBytesEntryTest.newDiff(seed));
    }

    @Override
    protected BytesConvertible.Parser<PatchChunkMessage<?>> getParser() {
        return PatchChunkMessage.getParser(this.idRegistry, this.diffRegistry);
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
