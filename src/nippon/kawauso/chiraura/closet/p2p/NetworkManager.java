/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.network.AddressAccessRequest;
import nippon.kawauso.chiraura.network.NetworkTask;
import nippon.kawauso.chiraura.network.PeerAccessRequest;

/**
 * 通信網の管理から発生した仕事を捌く人。
 * @author chirauraNoSakusha
 */
final class NetworkManager extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(NetworkManager.class.getName());

    // 参照。
    private final NetworkWrapper taskSource;
    private final long operationTimeout;
    private final PeerAccessNonBlockingDriver peerAccessDriver;
    private final AddressAccessNonBlockingDriver addressAccessDriver;

    NetworkManager(final BlockingQueue<Reporter.Report> reportSink, final NetworkWrapper taskSource, final long operationTimeout,
            final PeerAccessNonBlockingDriver peerAccessDriver, final AddressAccessNonBlockingDriver addressAccessDriver) {
        super(reportSink);

        if (taskSource == null) {
            throw new IllegalArgumentException("Null task source.");
        } else if (operationTimeout < 0) {
            throw new IllegalArgumentException("Negative operation timeout ( " + operationTimeout + " ).");
        } else if (addressAccessDriver == null) {
            throw new IllegalArgumentException("Null AddressAccess driver.");
        } else if (peerAccessDriver == null) {
            throw new IllegalArgumentException("Null PeerAccess driver.");
        }

        this.taskSource = taskSource;
        this.operationTimeout = operationTimeout;
        this.addressAccessDriver = addressAccessDriver;
        this.peerAccessDriver = peerAccessDriver;
    }

    @Override
    protected Void subCall() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            final NetworkTask task = this.taskSource.takeNetworkTask();

            boolean done = true;
            if (task instanceof AddressAccessRequest) {
                this.addressAccessDriver.execute(new AddressAccessOperation(((AddressAccessRequest) task).getAddress()), this.operationTimeout);
            } else if (task instanceof PeerAccessRequest) {
                this.peerAccessDriver.execute(new PeerAccessOperation(((PeerAccessRequest) task).getPeer()), this.operationTimeout);
            } else {
                done = false;
            }

            if (done) {
                LOG.log(Level.FINER, "{0} を捌きました。", task);
            } else {
                LOG.log(Level.WARNING, "{0} の処理は実装されていません。", task);
            }
        }

        return null;
    }
}
