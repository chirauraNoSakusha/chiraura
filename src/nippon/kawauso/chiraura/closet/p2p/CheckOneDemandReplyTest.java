/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class CheckOneDemandReplyTest extends UsingRegistryTest<CheckOneDemandReply> {

    @Override
    protected CheckOneDemandReply[] getInstances() {
        int seed = 0;
        return new CheckOneDemandReply[] {
                CheckOneDemandReply.newRejected(),
                CheckOneDemandReply.newGiveUp(),
                CheckOneDemandReply.newNoDemand(),
                new CheckOneDemandReply(DemandEntryTest.newInstance(this.idRegistry, seed++)),
        };
    }

    @Override
    protected CheckOneDemandReply getInstance(final int seed) {
        return new CheckOneDemandReply(DemandEntryTest.newInstance(this.idRegistry, seed));
    }

    @Override
    protected BytesConvertible.Parser<CheckOneDemandReply> getParser() {
        return CheckOneDemandReply.getParser(this.idRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
