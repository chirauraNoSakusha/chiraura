package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class SimpleRecoveryNonBlockingDriver {

    private static final Logger LOG = Logger.getLogger(SimpleRecoveryNonBlockingDriver.class.getName());

    private final OperationAggregator<SimpleRecoveryOperation, SimpleRecoveryResult> aggregator;
    private final SimpleRecoveryDriver coreDriver;
    private final ExecutorService executor;

    SimpleRecoveryNonBlockingDriver(final OperationAggregator<SimpleRecoveryOperation, SimpleRecoveryResult> aggregator, final SimpleRecoveryDriver coreDriver,
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

    void execute(final SimpleRecoveryOperation operation, final long timeout) throws IOException, InterruptedException {
        if (this.coreDriver.isObvious(operation)) {
            LOG.log(Level.FINEST, "{0} は自明に終わりました。", operation);
            return;
        }

        final CheckingStation.Instrument<SimpleRecoveryResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        // 自分で始める。
        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws IOException, InterruptedException {
                SimpleRecoveryResult result = null;
                try {
                    result = SimpleRecoveryNonBlockingDriver.this.coreDriver.execute(operation, timeout);
                } finally {
                    SimpleRecoveryNonBlockingDriver.this.aggregator.free(operation, result);
                }
                return null;
            }
        });
    }

}
