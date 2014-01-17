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
final class GetOrUpdateCacheBlockingDriver {

    private static final Logger LOG = Logger.getLogger(GetOrUpdateCacheBlockingDriver.class.getName());

    private final OperationAggregator<GetOrUpdateCacheOperation, GetOrUpdateCacheResult> aggregator;
    private final GetOrUpdateCacheDriver coreDriver;

    GetOrUpdateCacheBlockingDriver(final OperationAggregator<GetOrUpdateCacheOperation, GetOrUpdateCacheResult> aggregator,
            final GetOrUpdateCacheDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    GetOrUpdateCacheResult execute(final GetOrUpdateCacheOperation operation, final long timeout) throws InterruptedException, IOException {
        final CheckingStation.Instrument<GetOrUpdateCacheResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人の結果を待ちます。", operation);
            return instrument.get(timeout);
        }

        // 自分でやる。
        GetOrUpdateCacheResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }
        return result;
    }

}
