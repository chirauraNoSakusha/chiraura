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
final class GetChunkBlockingDriver {

    private static final Logger LOG = Logger.getLogger(GetChunkBlockingDriver.class.getName());

    // 参照。
    private final OperationAggregator<GetChunkOperation, GetChunkResult> aggregator;
    private final GetChunkDriver coreDriver;

    GetChunkBlockingDriver(final OperationAggregator<GetChunkOperation, GetChunkResult> aggregator, final GetChunkDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    GetChunkResult execute(final GetChunkOperation operation, final long timeout) throws IOException, InterruptedException {
        final CheckingStation.Instrument<GetChunkResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人の結果を待ちます。", operation);
            return instrument.get(timeout);
        }

        // 自分でやる。
        GetChunkResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }
        return result;
    }

}
