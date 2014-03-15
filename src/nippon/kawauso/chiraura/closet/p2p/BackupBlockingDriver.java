package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class BackupBlockingDriver {

    private static final Logger LOG = Logger.getLogger(BackupBlockingDriver.class.getName());

    private final OperationAggregator<BackupOperation, BackupResult> aggregator;
    private final BackupDriver coreDriver;

    BackupBlockingDriver(final OperationAggregator<BackupOperation, BackupResult> aggregator, final BackupDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    BackupResult execute(final BackupOperation operation, final long timeout) throws IOException, InterruptedException {
        final CheckingStation.Instrument<BackupResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人の結果を待ちます。", operation);
            return instrument.get(timeout);
        }

        // 自分でやる。
        BackupResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }

        return result;
    }

}
