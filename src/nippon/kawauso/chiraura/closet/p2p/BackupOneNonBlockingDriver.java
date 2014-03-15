package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class BackupOneNonBlockingDriver {

    private static final Logger LOG = Logger.getLogger(BackupOneNonBlockingDriver.class.getName());

    // 参照。
    private final OperationAggregator<BackupOneOperation, BackupOneResult> aggregator;
    private final BackupOneDriver coreDriver;
    private final ExecutorService executor;

    BackupOneNonBlockingDriver(final OperationAggregator<BackupOneOperation, BackupOneResult> aggregator, final BackupOneDriver coreDriver,
            final ExecutorService executor) {
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

    void execute(final BackupOneOperation operation, final long timeout) {
        final CheckingStation.Instrument<BackupOneResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        // 自分で始める。
        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws IOException, InterruptedException {
                BackupOneResult result = null;
                try {
                    result = BackupOneNonBlockingDriver.this.coreDriver.execute(operation, timeout);
                } finally {
                    BackupOneNonBlockingDriver.this.aggregator.free(operation, result);
                }
                return null;
            }
        });
    }

}
