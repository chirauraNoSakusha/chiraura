package nippon.kawauso.chiraura.closet.p2p;

import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class BackupNonBlockingDriver {

    private static final Logger LOG = Logger.getLogger(BackupNonBlockingDriver.class.getName());

    // 参照。
    private final OperationAggregator<BackupOperation, BackupResult> aggregator;
    private final BackupDriver coreDriver;
    private final ExecutorService executor;

    BackupNonBlockingDriver(final OperationAggregator<BackupOperation, BackupResult> aggregator, final BackupDriver coreDriver, final ExecutorService executor) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
        this.executor = executor;
    }

    void execute(final BackupOperation operation, final long timeout) {
        final CheckingStation.Instrument<BackupResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        // 自分で始める。
        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws Exception {
                BackupResult result = null;
                try {
                    result = BackupNonBlockingDriver.this.coreDriver.execute(operation, timeout);
                } finally {
                    BackupNonBlockingDriver.this.aggregator.free(operation, result);
                }
                return null;
            }
        });
    }

}
