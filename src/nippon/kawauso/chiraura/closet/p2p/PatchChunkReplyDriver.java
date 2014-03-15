package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class PatchChunkReplyDriver {

    private static final Logger LOG = Logger.getLogger(PatchChunkReplyDriver.class.getName());


    void execute(final PatchChunkReply reply) {
        LOG.log(Level.FINEST, "{0} に対してすることはありません。", reply);
    }

}
