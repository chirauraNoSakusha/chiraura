/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.Arrays;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
public final class CheckDemandReplyTest extends UsingRegistryTest<CheckDemandReply> {

    @Override
    protected CheckDemandReply[] getInstances() {
        int seed = 0;
        return new CheckDemandReply[] {
                CheckDemandReply.newRejected(),
                CheckDemandReply.newGiveUp(),
                new CheckDemandReply(Arrays.asList(DemandEntryTest.newInstance(this.idRegistry, seed++), DemandEntryTest.newInstance(this.idRegistry, seed++),
                        DemandEntryTest.newInstance(this.idRegistry, seed++))),
        };
    }

    static CheckDemandReply newInstance(final TypeRegistry<Chunk.Id<?>> idRegistry, final int seed) {
        return new CheckDemandReply(Arrays.asList(DemandEntryTest.newInstance(idRegistry, seed), DemandEntryTest.newInstance(idRegistry, seed + 1),
                DemandEntryTest.newInstance(idRegistry, seed + 2)));
    }

    @Override
    protected CheckDemandReply getInstance(final int seed) {
        return newInstance(this.idRegistry, seed);
    }

    @Override
    protected BytesConvertible.Parser<CheckDemandReply> getParser() {
        return CheckDemandReply.getParser(this.idRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
