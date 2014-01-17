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
final class PatchAndGetOrUpdateCacheBlockingDriver {

    private static final Logger LOG = Logger.getLogger(PatchAndGetOrUpdateCacheBlockingDriver.class.getName());

    private final OperationAggregator<PatchAndGetOrUpdateCacheOperation<?>, PatchAndGetOrUpdateCacheResult> aggregator;
    private final PatchAndGetOrUpdateCacheDriver coreDriver;

    PatchAndGetOrUpdateCacheBlockingDriver(final OperationAggregator<PatchAndGetOrUpdateCacheOperation<?>, PatchAndGetOrUpdateCacheResult> aggregator,
            final PatchAndGetOrUpdateCacheDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    PatchAndGetOrUpdateCacheResult execute(final PatchAndGetOrUpdateCacheOperation<?> operation, final long timeout) throws InterruptedException, IOException {
        final CheckingStation.Instrument<PatchAndGetOrUpdateCacheResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人の結果を待ちます。", operation);
            return instrument.get(timeout);
        }

        // 自分でやる。
        PatchAndGetOrUpdateCacheResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }
        return result;
    }

}
