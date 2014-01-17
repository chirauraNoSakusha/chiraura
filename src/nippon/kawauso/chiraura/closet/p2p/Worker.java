/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * 雑用係。
 * @author chirauraNoSakusha
 */
final class Worker extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(Worker.class.getName());

    // 参照。
    private final BlockingQueue<Operation> operationSource;
    private final long operationTimeout;

    private final NonBlockingDriverSet drivers;

    Worker(final BlockingQueue<? super Reporter.Report> reportSink, final BlockingQueue<Operation> operationSource,
            final long operationTimeout, final NonBlockingDriverSet drivers) {
        super(reportSink);
        if (operationSource == null) {
            throw new IllegalArgumentException("Null operation source.");
        } else if (operationTimeout < 0) {
            throw new IllegalArgumentException("Negative operation timeout ( " + operationTimeout + " ).");
        } else if (drivers == null) {
            throw new IllegalArgumentException("Null drivers.");
        }

        this.operationSource = operationSource;
        this.operationTimeout = operationTimeout;
        this.drivers = drivers;
    }

    @Override
    protected Void subCall() throws InterruptedException, IOException {
        while (!Thread.currentThread().isInterrupted()) {
            final Operation operation = this.operationSource.take();

            boolean done = true;
            if (operation instanceof PeerAccessOperation) {
                this.drivers.getPeerAccessNonBlocking().execute((PeerAccessOperation) operation, this.operationTimeout);
            } else if (operation instanceof AddressAccessOperation) {
                this.drivers.getAddressAccessNonBlocking().execute((AddressAccessOperation) operation, this.operationTimeout);
            } else if (operation instanceof GetChunkOperation) {
                this.drivers.getGetChunkNonBlocking().execute((GetChunkOperation) operation, this.operationTimeout);
            } else if (operation instanceof GetCacheOperation) {
                this.drivers.getGetCacheNonBlocking().execute((GetCacheOperation) operation, this.operationTimeout);
            } else if (operation instanceof UpdateChunkOperation) {
                this.drivers.getUpdateChunkNonBlocking().execute((UpdateChunkOperation) operation, this.operationTimeout);
            } else if (operation instanceof GetOrUpdateCacheOperation) {
                this.drivers.getGetOrUpdateCacheNonBlocking().execute((GetOrUpdateCacheOperation) operation, this.operationTimeout);
            } else if (operation instanceof AddChunkOperation) {
                this.drivers.getAddChunkNonBlocking().execute((AddChunkOperation) operation, this.operationTimeout);
            } else if (operation instanceof AddCacheOperation) {
                this.drivers.getAddCacheNonBlocking().execute((AddCacheOperation) operation, this.operationTimeout);
            } else if (operation instanceof PatchChunkOperation) {
                this.drivers.getPatchChunkNonBlocking().execute((PatchChunkOperation<?>) operation, this.operationTimeout);
            } else if (operation instanceof PatchOrAddAndGetCacheOperation) {
                this.drivers.getPatchOrAddAndGetCacheNonBlocking().execute((PatchOrAddAndGetCacheOperation) operation, this.operationTimeout);
            } else if (operation instanceof PatchAndGetOrUpdateCacheOperation) {
                this.drivers.getPatchAndGetOrUpdateCacheNonBlocking().execute((PatchAndGetOrUpdateCacheOperation<?>) operation, this.operationTimeout);
            } else if (operation instanceof BackupOneOperation) {
                this.drivers.getBackupOneNonBlocking().execute((BackupOneOperation) operation, this.operationTimeout);
            } else if (operation instanceof SimpleRecoveryOperation) {
                this.drivers.getSimpleRecoveryNonBlocking().execute((SimpleRecoveryOperation) operation, this.operationTimeout);
            } else {
                done = false;
            }

            if (done) {
                LOG.log(Level.FINER, "{0} を捌きました。", operation);
            } else {
                LOG.log(Level.WARNING, "{0} の処理は実装されていません。", operation);
            }
        }

        return null;
    }

}
