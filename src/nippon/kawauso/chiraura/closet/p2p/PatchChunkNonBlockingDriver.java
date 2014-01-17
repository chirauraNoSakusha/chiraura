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
final class PatchChunkNonBlockingDriver {

    private static final Logger LOG = Logger.getLogger(PatchChunkNonBlockingDriver.class.getName());

    private final OperationAggregator<PatchChunkOperation<?>, PatchChunkResult> aggregator;
    private final PatchChunkDriver coreDriver;
    private final ExecutorService executor;

    PatchChunkNonBlockingDriver(final OperationAggregator<PatchChunkOperation<?>, PatchChunkResult> aggregator, final PatchChunkDriver coreDriver,
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

    void execute(final PatchChunkOperation<?> operation, final long timeout) {
        final CheckingStation.Instrument<PatchChunkResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        // 自分で始める。
        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws Exception {
                PatchChunkResult result = null;
                try {
                    result = PatchChunkNonBlockingDriver.this.coreDriver.execute(operation, timeout);
                } finally {
                    PatchChunkNonBlockingDriver.this.aggregator.free(operation, result);
                }
                return null;
            }
        });
    }

}
