package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.storage.ConstantChunkTest;
import nippon.kawauso.chiraura.storage.VariableChunkTest;

/**
 * @author chirauraNoSakusha
 */
public final class RecoveryMessageTest extends UsingRegistryTest<RecoveryMessage> {

    @Override
    protected RecoveryMessage[] getInstances() {
        int seed = 0;
        return new RecoveryMessage[] {
                new RecoveryMessage(this.idRegistry, ConstantChunkTest.newId(seed++)),
                new RecoveryMessage(this.idRegistry, VariableChunkTest.newId(seed++)),
                new RecoveryMessage(this.idRegistry, GrowingBytesTest.newId(seed++)),
                new RecoveryMessage(this.idRegistry, ConstantChunkTest.newId(seed++), seed++),
                new RecoveryMessage(this.idRegistry, VariableChunkTest.newId(seed++), seed++),
                new RecoveryMessage(this.idRegistry, GrowingBytesTest.newId(seed++), seed++),
        };
    }

    @Override
    protected RecoveryMessage getInstance(final int seed) {
        return new RecoveryMessage(this.idRegistry, GrowingBytesTest.newId(seed), seed);
    }

    @Override
    protected BytesConvertible.Parser<RecoveryMessage> getParser() {
        return RecoveryMessage.getParser(this.idRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
