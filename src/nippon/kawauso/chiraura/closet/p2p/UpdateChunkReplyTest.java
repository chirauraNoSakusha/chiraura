/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.ArrayList;
import java.util.Arrays;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * @author chirauraNoSakusha
 */
public final class UpdateChunkReplyTest extends UsingRegistryTest<UpdateChunkReply> {

    @Override
    protected UpdateChunkReply[] getInstances() {
        int seed = 0;
        return new UpdateChunkReply[] {
                UpdateChunkReply.newGiveUp(),
                UpdateChunkReply.newNotFound(),
                new UpdateChunkReply(this.diffRegistry, new ArrayList<Mountain.Dust<?>>(0)),
                new UpdateChunkReply(this.diffRegistry, Arrays.asList(GrowingBytesEntryTest.newDiff(seed++), GrowingBytesEntryTest.newDiff(seed++),
                        GrowingBytesEntryTest.newDiff(seed++))),
        };
    }

    @Override
    protected UpdateChunkReply getInstance(final int seed) {
        return new UpdateChunkReply(this.diffRegistry, Arrays.asList(GrowingBytesEntryTest.newDiff(seed), GrowingBytesEntryTest.newDiff(seed + 1),
                GrowingBytesEntryTest.newDiff(seed + 2)));
    }

    @Override
    protected BytesConvertible.Parser<UpdateChunkReply> getParser() {
        return UpdateChunkReply.getParser(this.diffRegistry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
