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
final class PatchOrAddAndGetCacheBlockingDriver {

    private static final Logger LOG = Logger.getLogger(PatchOrAddAndGetCacheBlockingDriver.class.getName());

    private final OperationAggregator<PatchOrAddAndGetCacheOperation, PatchOrAddAndGetCacheResult> aggregator;
    private final PatchOrAddAndGetCacheDriver coreDriver;

    PatchOrAddAndGetCacheBlockingDriver(final OperationAggregator<PatchOrAddAndGetCacheOperation, PatchOrAddAndGetCacheResult> aggregator,
            final PatchOrAddAndGetCacheDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    PatchOrAddAndGetCacheResult execute(final PatchOrAddAndGetCacheOperation operation, final long timeout) throws InterruptedException, IOException {
        final CheckingStation.Instrument<PatchOrAddAndGetCacheResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人の結果を待ちます。", operation);
            return instrument.get(timeout);
        }

        // 自分でやる。
        PatchOrAddAndGetCacheResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }
        return result;
    }

}
