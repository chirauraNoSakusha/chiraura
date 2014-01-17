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
public final class RecoveryReplyTest extends UsingRegistryTest<RecoveryReply<?>> {

    @Override
    protected RecoveryReply<?>[] getInstances() {
        int seed = 0;
        final GrowingBytes chunk = GrowingBytesTest.newInstance(seed++);
        return new RecoveryReply[] {
                RecoveryReply.newRejected(),
                RecoveryReply.newNotFound(),
                new RecoveryReply<>(this.chunkRegistry, ConstantChunkTest.newInstance(seed++)),
                new RecoveryReply<>(this.chunkRegistry, VariableChunkTest.newInstance(seed++)),
                new RecoveryReply<>(this.chunkRegistry, GrowingBytesTest.newInstance(seed++)),
                new RecoveryReply<>(this.chunkRegistry, chunk, chunk.getDate()),
                new RecoveryReply<>(this.chunkRegistry, chunk, Long.MIN_VALUE),
        };
    }

    @Override
    protected RecoveryReply<?> getInstance(final int seed) {
        final GrowingBytes chunk = GrowingBytesTest.newInstance(seed);
        return new RecoveryReply<>(this.chunkRegistry, chunk, Long.MIN_VALUE);
    }

    @Override
    protected BytesConvertible.Parser<RecoveryReply<?>> getParser() {
        return RecoveryReply.getParser(this.chunkRegistry, this.idRegistry, this.diffRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
