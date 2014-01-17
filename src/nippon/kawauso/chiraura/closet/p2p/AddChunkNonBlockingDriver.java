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
final class AddChunkNonBlockingDriver {

    private static final Logger LOG = Logger.getLogger(AddChunkNonBlockingDriver.class.getName());

    private final OperationAggregator<AddChunkOperation, AddChunkResult> aggregator;
    private final AddChunkDriver coreDriver;
    private final ExecutorService executor;

    AddChunkNonBlockingDriver(final OperationAggregator<AddChunkOperation, AddChunkResult> aggregator, final AddChunkDriver coreDriver,
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

    void execute(final AddChunkOperation operation, final long timeout) {
        final CheckingStation.Instrument<AddChunkResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        // 自分で始める。
        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws Exception {
                AddChunkResult result = null;
                try {
                    result = AddChunkNonBlockingDriver.this.coreDriver.execute(operation, timeout);
                } finally {
                    AddChunkNonBlockingDriver.this.aggregator.free(operation, result);
                }
                return null;
            }
        });
    }

}
