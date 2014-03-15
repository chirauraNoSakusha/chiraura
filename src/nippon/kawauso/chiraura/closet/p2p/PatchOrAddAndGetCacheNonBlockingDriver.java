package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class PatchOrAddAndGetCacheNonBlockingDriver {

    private static final Logger LOG = Logger.getLogger(PatchOrAddAndGetCacheNonBlockingDriver.class.getName());

    private final OperationAggregator<PatchOrAddAndGetCacheOperation, PatchOrAddAndGetCacheResult> aggregator;
    private final PatchOrAddAndGetCacheDriver coreDriver;
    private final ExecutorService executor;

    PatchOrAddAndGetCacheNonBlockingDriver(final OperationAggregator<PatchOrAddAndGetCacheOperation, PatchOrAddAndGetCacheResult> aggregator,
            final PatchOrAddAndGetCacheDriver coreDriver, final ExecutorService executor) {
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

    void execute(final PatchOrAddAndGetCacheOperation operation, final long timeout) {
        final CheckingStation.Instrument<PatchOrAddAndGetCacheResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        // 自分で始める。
        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws InterruptedException, IOException {
                PatchOrAddAndGetCacheResult result = null;
                try {
                    result = PatchOrAddAndGetCacheNonBlockingDriver.this.coreDriver.execute(operation, timeout);
                } finally {
                    PatchOrAddAndGetCacheNonBlockingDriver.this.aggregator.free(operation, result);
                }
                return null;
            }
        });
    }

}
