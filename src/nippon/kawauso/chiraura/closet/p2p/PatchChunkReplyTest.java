/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class PatchChunkReplyTest extends UsingRegistryTest<PatchChunkReply> {

    @Override
    protected PatchChunkReply[] getInstances() {
        return new PatchChunkReply[] {
                PatchChunkReply.newRejected(),
                PatchChunkReply.newGiveUp(),
                PatchChunkReply.newNotFound(),
                PatchChunkReply.newFailure(),
                new PatchChunkReply(),
        };
    }

    @Override
    protected PatchChunkReply getInstance(final int seed) {
        return new PatchChunkReply();
    }

    @Override
    protected BytesConvertible.Parser<PatchChunkReply> getParser() {
        return PatchChunkReply.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 10;
    }

}
