/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class AddChunkReplyTest extends UsingRegistryTest<AddChunkReply> {

    @Override
    protected AddChunkReply[] getInstances() {
        return new AddChunkReply[] {
                AddChunkReply.newRejected(),
                AddChunkReply.newGiveUp(),
                AddChunkReply.newFailure(),
                new AddChunkReply(),
        };
    }

    @Override
    protected AddChunkReply getInstance(final int seed) {
        return new AddChunkReply();
    }

    @Override
    protected BytesConvertible.Parser<AddChunkReply> getParser() {
        return AddChunkReply.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 10;
    }

}
