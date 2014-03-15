package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class PatchChunkBlockingDriver {

    private static final Logger LOG = Logger.getLogger(PatchChunkBlockingDriver.class.getName());

    private final OperationAggregator<PatchChunkOperation<?>, PatchChunkResult> aggregator;
    private final PatchChunkDriver coreDriver;

    PatchChunkBlockingDriver(final OperationAggregator<PatchChunkOperation<?>, PatchChunkResult> aggregator, final PatchChunkDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    PatchChunkResult execute(final PatchChunkOperation<?> operation, final long timeout) throws InterruptedException, IOException {
        final CheckingStation.Instrument<PatchChunkResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人の結果を待ちます。", operation);
            return instrument.get(timeout);
        }

        // 自分でやる。
        PatchChunkResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }
        return result;
    }

}
