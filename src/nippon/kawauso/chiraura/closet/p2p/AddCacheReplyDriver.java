/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class AddCacheReplyDriver {

    private static final Logger LOG = Logger.getLogger(AddCacheReplyDriver.class.getName());

    void execute(final AddCacheReply reply) {
        LOG.log(Level.FINEST, "{0} に対してすることはありません。", reply);
    }

}
