package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.storage.ConstantChunkTest;
import nippon.kawauso.chiraura.storage.VariableChunkTest;

/**
 * @author chirauraNoSakusha
 */
public final class BackupMessageTest extends UsingRegistryTest<BackupMessage<?>> {

    @Override
    protected BackupMessage<?>[] getInstances() {
        int seed = 0;
        final GrowingBytes chunk = GrowingBytesTest.newInstance(seed++);
        return new BackupMessage<?>[] {
                new BackupMessage<>(this.chunkRegistry, ConstantChunkTest.newInstance(seed++)),
                new BackupMessage<>(this.chunkRegistry, VariableChunkTest.newInstance(seed++)),
                new BackupMessage<>(this.chunkRegistry, GrowingBytesTest.newInstance(seed++)),
                new BackupMessage<>(this.chunkRegistry, chunk, chunk.getDate()),
                new BackupMessage<>(this.chunkRegistry, chunk, Long.MIN_VALUE),
        };
    }

    @Override
    protected BackupMessage<?> getInstance(final int seed) {
        return new BackupMessage<>(this.chunkRegistry, GrowingBytesTest.newInstance(seed), Long.MIN_VALUE);
    }

    @Override
    protected BytesConvertible.Parser<BackupMessage<?>> getParser() {
        return BackupMessage.getParser(this.chunkRegistry, this.idRegistry, this.diffRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
