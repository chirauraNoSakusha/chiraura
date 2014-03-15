package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class CheckOneDemandBlockingDriver {

    private static final Logger LOG = Logger.getLogger(CheckOneDemandBlockingDriver.class.getName());

    private final OperationAggregator<CheckOneDemandOperation, CheckOneDemandResult> aggregator;
    private final CheckOneDemandDriver coreDriver;

    CheckOneDemandBlockingDriver(final OperationAggregator<CheckOneDemandOperation, CheckOneDemandResult> aggregator, final CheckOneDemandDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    CheckOneDemandResult execute(final CheckOneDemandOperation operation, final long timeout) throws IOException, InterruptedException {
        final CheckingStation.Instrument<CheckOneDemandResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人の結果を待ちます。", operation);
            return instrument.get(timeout);
        }

        // 自分でやる。
        CheckOneDemandResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }

        return result;
    }

}
