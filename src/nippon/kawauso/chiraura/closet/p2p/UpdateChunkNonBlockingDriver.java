package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class UpdateChunkNonBlockingDriver {

    private static final Logger LOG = Logger.getLogger(UpdateChunkNonBlockingDriver.class.getName());

    private final OperationAggregator<UpdateChunkOperation, UpdateChunkResult> aggregator;
    private final UpdateChunkDriver coreDriver;
    private final ExecutorService executor;

    UpdateChunkNonBlockingDriver(final OperationAggregator<UpdateChunkOperation, UpdateChunkResult> aggregator, final UpdateChunkDriver coreDriver,
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

    void execute(final UpdateChunkOperation operation, final long timeout) {
        if (this.coreDriver.isObvious(operation)) {
            LOG.log(Level.FINEST, "{0} は自明に終わりました。", operation);
            return;
        }

        final CheckingStation.Instrument<UpdateChunkResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        // 自分で始める。
        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws IOException, InterruptedException {
                UpdateChunkResult result = null;
                try {
                    result = UpdateChunkNonBlockingDriver.this.coreDriver.execute(operation, timeout);
                } finally {
                    UpdateChunkNonBlockingDriver.this.aggregator.free(operation, result);
                }
                return null;
            }
        });
    }

}
