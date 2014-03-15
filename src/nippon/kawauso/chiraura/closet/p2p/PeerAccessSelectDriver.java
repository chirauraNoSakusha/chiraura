package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 個体を確認する。
 * @author chirauraNoSakusha
 */
final class PeerAccessSelectDriver {

    private static final Logger LOG = Logger.getLogger(PeerAccessSelectDriver.class.getName());

    // 参照。
    private final OperationAggregator<PeerAccessOperation, PeerAccessResult> aggregator;
    private final PeerAccessDriver coreDriver;

    PeerAccessSelectDriver(final OperationAggregator<PeerAccessOperation, PeerAccessResult> aggregator, final PeerAccessDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null operation aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }

        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    void execute(final PeerAccessOperation operation, final long timeout) throws InterruptedException {
        final CheckingStation.Instrument<PeerAccessResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            // 既に誰かがやってる最中だから任せる。
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        PeerAccessResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }
        return;
    }

}
