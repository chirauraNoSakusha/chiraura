package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class CheckOneDemandReplyDriver {

    private static final Logger LOG = Logger.getLogger(CheckOneDemandReplyDriver.class.getName());


    void execute(final CheckOneDemandReply reply) {
        LOG.log(Level.FINEST, "{0} に対してすることはありません。", reply);
    }

}
