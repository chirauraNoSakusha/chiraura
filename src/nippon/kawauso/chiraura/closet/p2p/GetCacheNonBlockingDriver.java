/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class GetCacheNonBlockingDriver {

    private static final Logger LOG = Logger.getLogger(GetCacheNonBlockingDriver.class.getName());

    // 参照。
    private final OperationAggregator<GetCacheOperation, GetCacheResult> aggregator;
    private final GetCacheDriver coreDriver;
    private final ExecutorService executor;

    GetCacheNonBlockingDriver(final OperationAggregator<GetCacheOperation, GetCacheResult> aggregator, final GetCacheDriver coreDriver,
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

    void execute(final GetCacheOperation operation, final long timeout) throws InterruptedException, IOException {
        if (this.coreDriver.isObvious(operation)) {
            LOG.log(Level.FINEST, "{0} の結果は自明です。", operation);
            return;
        }

        final CheckingStation.Instrument<GetCacheResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws Exception {
                GetCacheResult result = null;
                try {
                    result = GetCacheNonBlockingDriver.this.coreDriver.execute(operation, timeout);
                } finally {
                    GetCacheNonBlockingDriver.this.aggregator.free(operation, result);
                }
                return null;
            }
        });
    }

}
