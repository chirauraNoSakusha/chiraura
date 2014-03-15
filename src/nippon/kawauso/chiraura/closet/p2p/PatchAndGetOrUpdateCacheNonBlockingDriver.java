package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class PatchAndGetOrUpdateCacheNonBlockingDriver {

    private static final Logger LOG = Logger.getLogger(PatchAndGetOrUpdateCacheNonBlockingDriver.class.getName());

    private final OperationAggregator<PatchAndGetOrUpdateCacheOperation<?>, PatchAndGetOrUpdateCacheResult> aggregator;
    private final PatchAndGetOrUpdateCacheDriver coreDriver;
    private final ExecutorService executor;

    PatchAndGetOrUpdateCacheNonBlockingDriver(final OperationAggregator<PatchAndGetOrUpdateCacheOperation<?>, PatchAndGetOrUpdateCacheResult> aggregator,
            final PatchAndGetOrUpdateCacheDriver coreDriver,
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

    void execute(final PatchAndGetOrUpdateCacheOperation<?> operation, final long timeout) {
        final CheckingStation.Instrument<PatchAndGetOrUpdateCacheResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        // 自分で始める。
        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws InterruptedException, IOException {
                PatchAndGetOrUpdateCacheResult result = null;
                try {
                    result = PatchAndGetOrUpdateCacheNonBlockingDriver.this.coreDriver.execute(operation, timeout);
                } finally {
                    PatchAndGetOrUpdateCacheNonBlockingDriver.this.aggregator.free(operation, result);
                }
                return null;
            }
        });
    }

}
