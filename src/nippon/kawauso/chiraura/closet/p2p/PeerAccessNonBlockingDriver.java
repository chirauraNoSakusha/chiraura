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
final class PeerAccessNonBlockingDriver {

    private static final Logger LOG = Logger.getLogger(PeerAccessNonBlockingDriver.class.getName());

    // 参照。
    private final OperationAggregator<PeerAccessOperation, PeerAccessResult> aggregator;
    private final PeerAccessDriver coreDriver;
    private final ExecutorService executor;

    PeerAccessNonBlockingDriver(final OperationAggregator<PeerAccessOperation, PeerAccessResult> aggregator, final PeerAccessDriver coreDriver,
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

    void execute(final PeerAccessOperation operation, final long timeout) {
        final CheckingStation.Instrument<PeerAccessResult> instrument = this.aggregator.register(operation);
        if (instrument != null) {
            LOG.log(Level.FINER, "{0} を先人に任せます。", operation);
            return;
        }

        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            public Void subCall() throws InterruptedException {
                PeerAccessResult result = null;
                try {
                    result = PeerAccessNonBlockingDriver.this.coreDriver.execute(operation, timeout);
                } finally {
                    PeerAccessNonBlockingDriver.this.aggregator.free(operation, result);
                }
                return null;
            }
        });
    }

}
