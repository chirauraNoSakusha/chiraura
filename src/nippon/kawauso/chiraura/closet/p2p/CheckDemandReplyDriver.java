/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class CheckDemandReplyDriver {

    private static final Logger LOG = Logger.getLogger(CheckDemandReplyDriver.class.getName());

    
    void execute(final CheckDemandReply reply) {
        LOG.log(Level.FINEST, "{0} に対してすることはありません。", reply);
    }

}
