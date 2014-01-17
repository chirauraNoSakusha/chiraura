/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
final class FirstAccessSelectDriver {

    private static final Logger LOG = Logger.getLogger(FirstAccessSelectDriver.class.getName());

    // 参照。
    private final OperationAggregator<FirstAccessOperation, FirstAccessResult> aggregator;
    private final FirstAccessDriver coreDriver;

    FirstAccessSelectDriver(final OperationAggregator<FirstAccessOperation, FirstAccessResult> aggregator, final FirstAccessDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null operation aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }

        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    void execute(final FirstAccessOperation operation, final long timeout) throws InterruptedException {
        final CheckingStation.Instrument<FirstAccessResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            // 既に誰かがやってる最中だから任せる。
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        FirstAccessResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }
        return;
    }

}
