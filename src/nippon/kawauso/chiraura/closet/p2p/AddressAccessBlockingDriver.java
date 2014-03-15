package nippon.kawauso.chiraura.closet.p2p;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 論理位置の支配者を確認する。
 * @author chirauraNoSakusha
 */
final class AddressAccessBlockingDriver {

    private static final Logger LOG = Logger.getLogger(AddressAccessBlockingDriver.class.getName());

    private final OperationAggregator<AddressAccessOperation, AddressAccessResult> aggregator;
    private final AddressAccessDriver coreDriver;

    AddressAccessBlockingDriver(final OperationAggregator<AddressAccessOperation, AddressAccessResult> aggregator, final AddressAccessDriver coreDriver) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        }
        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
    }

    AddressAccessResult execute(final AddressAccessOperation operation, final long timeout) throws InterruptedException {
        final CheckingStation.Instrument<AddressAccessResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人の結果を待ちます。", operation);
            return instrument.get(timeout);
        }

        AddressAccessResult result = null;
        try {
            result = this.coreDriver.execute(operation, timeout);
        } finally {
            this.aggregator.free(operation, result);
        }
        return result;
    }
}
