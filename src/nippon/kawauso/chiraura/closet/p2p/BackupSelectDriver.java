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
final class BackupSelectDriver {

    private static final Logger LOG = Logger.getLogger(BackupSelectDriver.class.getName());

    // 参照。
    private final OperationAggregator<BackupOperation, BackupResult> aggregator;
    private final BackupDriver coreDriver;

    BackupSelectDriver(final OperationAggregator<BackupOperation, BackupResult> aggregator, final BackupDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    void execute(final BackupOperation operation, final long timeout) throws InterruptedException, IOException {
        final CheckingStation.Instrument<BackupResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        // 自分でやる。
        BackupResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }
    }

}
