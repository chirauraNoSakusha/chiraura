/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class UpdateChunkBlockingDriver {

    private static final Logger LOG = Logger.getLogger(UpdateChunkBlockingDriver.class.getName());

    private final OperationAggregator<UpdateChunkOperation, UpdateChunkResult> aggregator;
    private final UpdateChunkDriver coreDriver;

    UpdateChunkBlockingDriver(final OperationAggregator<UpdateChunkOperation, UpdateChunkResult> aggregator, final UpdateChunkDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    UpdateChunkResult execute(final UpdateChunkOperation operation, final long timeout) throws IOException, InterruptedException {
        final CheckingStation.Instrument<UpdateChunkResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人の結果を待ちます。", operation);
            return instrument.get(timeout);
        }

        // 自分でやる。
        UpdateChunkResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }
        return result;
    }

}
