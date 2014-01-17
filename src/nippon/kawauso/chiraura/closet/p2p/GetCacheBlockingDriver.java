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
final class GetCacheBlockingDriver {

    private static final Logger LOG = Logger.getLogger(GetCacheBlockingDriver.class.getName());

    private final OperationAggregator<GetCacheOperation, GetCacheResult> aggregator;
    private final GetCacheDriver coreDriver;

    GetCacheBlockingDriver(final OperationAggregator<GetCacheOperation, GetCacheResult> aggregator, final GetCacheDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    GetCacheResult execute(final GetCacheOperation operation, final long timeout) throws IOException, InterruptedException {
        final CheckingStation.Instrument<GetCacheResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人の結果を待ちます。", operation);
            return instrument.get(timeout);
        }

        GetCacheResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }
        return result;
    }

}
