package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class AddChunkBlockingDriver {

    private static final Logger LOG = Logger.getLogger(AddChunkBlockingDriver.class.getName());

    private final OperationAggregator<AddChunkOperation, AddChunkResult> aggregator;
    private final AddChunkDriver coreDriver;

    AddChunkBlockingDriver(final OperationAggregator<AddChunkOperation, AddChunkResult> aggregator, final AddChunkDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    AddChunkResult execute(final AddChunkOperation operation, final long timeout) throws IOException, InterruptedException {
        final CheckingStation.Instrument<AddChunkResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人の結果を待ちます。", operation);
            return instrument.get(timeout);
        }

        // 自分でやる。
        AddChunkResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }

        return result;
    }

}
