/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class AddChunkReplyDriver {

    private static final Logger LOG = Logger.getLogger(AddChunkReplyDriver.class.getName());

    void execute(final AddChunkReply reply) {
        LOG.log(Level.FINEST, "{0} に対してすることはありません。", reply);
    }

}
