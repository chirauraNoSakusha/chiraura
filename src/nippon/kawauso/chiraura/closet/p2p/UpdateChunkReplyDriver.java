package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class UpdateChunkReplyDriver {

    private static final Logger LOG = Logger.getLogger(UpdateChunkReplyDriver.class.getName());


    void execute(final UpdateChunkReply reply) {
        LOG.log(Level.FINEST, "{0} に対してすることはありません。", reply);
    }

}
