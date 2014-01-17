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
final class AddCacheBlockingDriver {

    private static final Logger LOG = Logger.getLogger(AddCacheBlockingDriver.class.getName());

    private final OperationAggregator<AddCacheOperation, AddCacheResult> aggregator;
    private final AddCacheDriver coreDriver;

    AddCacheBlockingDriver(final OperationAggregator<AddCacheOperation, AddCacheResult> aggregator, final AddCacheDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    AddCacheResult execute(final AddCacheOperation operation, final long timeout) throws IOException, InterruptedException {
        final CheckingStation.Instrument<AddCacheResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人の結果を待ちます。", operation);
            return instrument.get(timeout);
        }

        // 自分でやる。
        AddCacheResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }

        return result;
    }

}
