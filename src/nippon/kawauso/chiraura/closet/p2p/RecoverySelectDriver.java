/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class RecoverySelectDriver {

    private static final Logger LOG = Logger.getLogger(RecoverySelectDriver.class.getName());

    // 参照。
    private final OperationAggregator<RecoveryOperation, RecoveryResult> aggregator;
    private final RecoveryDriver coreDriver;

    RecoverySelectDriver(final OperationAggregator<RecoveryOperation, RecoveryResult> aggregator, final RecoveryDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    void execute(final RecoveryOperation operation, final long timeout) throws InterruptedException, IOException {
        final CheckingStation.Instrument<RecoveryResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            // 既に誰かがやってるから任せる。
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        // 自分でやる。
        RecoveryResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }
    }

}
