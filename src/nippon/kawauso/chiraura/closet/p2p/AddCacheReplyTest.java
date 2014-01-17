/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class AddCacheReplyTest extends UsingRegistryTest<AddCacheReply> {

    @Override
    protected AddCacheReply[] getInstances() {
        int seed = 0;
        return new AddCacheReply[] {
                AddCacheReply.newRejected(),
                AddCacheReply.newGiveUp(),
                AddCacheReply.newFailure(),
                new AddCacheReply(seed++),
        };
    }

    @Override
    protected AddCacheReply getInstance(final int seed) {
        return new AddCacheReply(seed);
    }

    @Override
    protected BytesConvertible.Parser<AddCacheReply> getParser() {
        return AddCacheReply.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
