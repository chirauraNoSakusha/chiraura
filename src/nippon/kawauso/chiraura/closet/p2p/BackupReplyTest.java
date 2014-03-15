package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.storage.ConstantChunkTest;
import nippon.kawauso.chiraura.storage.VariableChunkTest;

/**
 * @author chirauraNoSakusha
 */
public final class BackupReplyTest extends UsingRegistryTest<BackupReply> {

    @Override
    protected BackupReply[] getInstances() {
        int seed = 0;
        return new BackupReply[] {
                BackupReply.newRejected(),
                BackupReply.newGiveUp(),
                BackupReply.newFailure(this.chunkRegistry, ConstantChunkTest.newInstance(seed++)),
                BackupReply.newFailure(this.chunkRegistry, VariableChunkTest.newInstance(seed++)),
                BackupReply.newFailure(this.chunkRegistry, GrowingBytesTest.newInstance(seed++)),
                new BackupReply(),
        };
    }

    @Override
    protected BackupReply getInstance(final int seed) {
        return BackupReply.newFailure(this.chunkRegistry, GrowingBytesTest.newInstance(seed));
    }

    @Override
    protected BytesConvertible.Parser<BackupReply> getParser() {
        return BackupReply.getParser(this.chunkRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
