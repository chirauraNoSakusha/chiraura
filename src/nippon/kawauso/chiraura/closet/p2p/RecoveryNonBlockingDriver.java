/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class RecoveryNonBlockingDriver {

    private static final Logger LOG = Logger.getLogger(RecoveryNonBlockingDriver.class.getName());

    // 参照。
    private final OperationAggregator<RecoveryOperation, RecoveryResult> aggregator;
    private final RecoveryDriver coreDriver;
    private final ExecutorService executor;

    RecoveryNonBlockingDriver(final OperationAggregator<RecoveryOperation, RecoveryResult> aggregator, final RecoveryDriver coreDriver,
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

    void execute(final RecoveryOperation operation, final long timeout) {
        final CheckingStation.Instrument<RecoveryResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            // 既に誰かがやってる最中だから任せる。
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        // 自分で始める。
        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws Exception {
                RecoveryResult result = null;
                try {
                    result = RecoveryNonBlockingDriver.this.coreDriver.execute(operation, timeout);
                } finally {
                    RecoveryNonBlockingDriver.this.aggregator.free(operation, result);
                }
                return null;
            }
        });
    }

}
