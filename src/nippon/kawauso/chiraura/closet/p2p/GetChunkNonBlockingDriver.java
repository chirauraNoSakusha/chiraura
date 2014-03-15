package nippon.kawauso.chiraura.closet.p2p;

import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class GetChunkNonBlockingDriver {

    private static final Logger LOG = Logger.getLogger(GetChunkNonBlockingDriver.class.getName());

    // 参照。
    private final OperationAggregator<GetChunkOperation, GetChunkResult> aggregator;
    private final GetChunkDriver coreDriver;
    private final ExecutorService executor;

    GetChunkNonBlockingDriver(final OperationAggregator<GetChunkOperation, GetChunkResult> aggregator, final GetChunkDriver coreDriver,
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

    void execute(final GetChunkOperation operation, final long timeout) {
        if (this.coreDriver.isObvious(operation)) {
            LOG.log(Level.FINEST, "{0} の結果は自明です。", operation);
            return;
        }

        final CheckingStation.Instrument<GetChunkResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws Exception {
                GetChunkResult result = null;
                // 自分でやる。
                try {
                    result = GetChunkNonBlockingDriver.this.coreDriver.execute(operation, timeout);
                } finally {
                    GetChunkNonBlockingDriver.this.aggregator.free(operation, result);
                }
                return null;
            }
        });
    }

}
