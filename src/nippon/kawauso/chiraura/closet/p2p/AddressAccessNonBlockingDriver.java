/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * 論理位置の支配者を確認する。
 * 通信が必要なら、別スレッドに移行する。
 * @author chirauraNoSakusha
 */
final class AddressAccessNonBlockingDriver {

    private static final Logger LOG = Logger.getLogger(AddressAccessNonBlockingDriver.class.getName());

    private final OperationAggregator<AddressAccessOperation, AddressAccessResult> aggregator;
    private final AddressAccessDriver coreDriver;
    private final ExecutorService executor;

    AddressAccessNonBlockingDriver(final OperationAggregator<AddressAccessOperation, AddressAccessResult> aggregator, final AddressAccessDriver coreDriver,
            final ExecutorService executor) {
        if (aggregator == null) {
            throw new IllegalArgumentException("Null operation aggregator.");
        } else if (coreDriver == null) {
            throw new IllegalArgumentException("Null core driver.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        }

        this.aggregator = aggregator;
        this.coreDriver = coreDriver;
        this.executor = executor;
    }

    void execute(final AddressAccessOperation operation, final long timeout) {
        if (this.coreDriver.isObvious(operation)) {
            LOG.log(Level.FINEST, "{0} は自明です。", operation);
            return;
        }

        final CheckingStation.Instrument<AddressAccessResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINEST, "{0} は先人に任せます。", operation);
            return;
        }

        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            public Void subCall() throws InterruptedException {
                AddressAccessResult result = null;
                try {
                    result = AddressAccessNonBlockingDriver.this.coreDriver.execute(operation, timeout);
                } finally {
                    AddressAccessNonBlockingDriver.this.aggregator.free(operation, result);
                }
                return null;
            }
        });
    }

}
