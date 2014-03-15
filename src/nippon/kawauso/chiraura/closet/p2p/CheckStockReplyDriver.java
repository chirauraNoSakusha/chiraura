package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class CheckStockReplyDriver {

    private static final Logger LOG = Logger.getLogger(CheckStockReplyDriver.class.getName());

    void execute(final CheckStockReply reply) {
        LOG.log(Level.FINEST, "{0} に対してすることはありません。", reply);
    }

}
