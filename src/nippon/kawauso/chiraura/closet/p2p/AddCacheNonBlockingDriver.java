package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class AddCacheNonBlockingDriver {

    private static final Logger LOG = Logger.getLogger(AddCacheNonBlockingDriver.class.getName());

    private final OperationAggregator<AddCacheOperation, AddCacheResult> aggregator;
    private final AddCacheDriver coreDriver;
    private final ExecutorService executor;

    AddCacheNonBlockingDriver(final OperationAggregator<AddCacheOperation, AddCacheResult> aggregator, final AddCacheDriver coreDriver,
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

    void execute(final AddCacheOperation operation, final long timeout) throws IOException, InterruptedException {
        if (this.coreDriver.isObvious(operation)) {
            LOG.log(Level.FINEST, "{0} の結果は自明です。", operation);
            return;
        }

        final CheckingStation.Instrument<AddCacheResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        // 自分で始める。
        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws Exception {
                AddCacheResult result = null;
                try {
                    result = AddCacheNonBlockingDriver.this.coreDriver.execute(operation, timeout);
                } finally {
                    AddCacheNonBlockingDriver.this.aggregator.free(operation, result);
                }
                return null;
            }
        });
    }

}
