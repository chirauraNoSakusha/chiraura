package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class GetChunkReplyDriver {

    private static final Logger LOG = Logger.getLogger(GetChunkReplyDriver.class.getName());


    void execute(final GetChunkReply reply) {
        LOG.log(Level.FINEST, "{0} に対してすることはありません。", reply);
    }

}
