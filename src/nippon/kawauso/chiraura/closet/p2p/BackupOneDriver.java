/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * @author chirauraNoSakusha
 */
final class BackupOneDriver {

    private static final Logger LOG = Logger.getLogger(BackupOneDriver.class.getName());

    private final NetworkWrapper network;
    private final CheckOneDemandBlockingDriver checkOneDemandBlockingDriver;
    private final BackupBlockingDriver backupBlockingDriver;

    BackupOneDriver(final NetworkWrapper network, final CheckOneDemandBlockingDriver checkOneDemandBlockingDriver,
            final BackupBlockingDriver backupBlockingDriver) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (checkOneDemandBlockingDriver == null) {
            throw new IllegalArgumentException("Null CheckOneDemand blocking driver.");
        } else if (backupBlockingDriver == null) {
            throw new IllegalArgumentException("Null Backup blocking driver.");
        }
        this.network = network;
        this.checkOneDemandBlockingDriver = checkOneDemandBlockingDriver;
        this.backupBlockingDriver = backupBlockingDriver;
    }

    BackupOneResult execute(final BackupOneOperation operation, final long timeout) throws IOException, InterruptedException {
        final long start = System.currentTimeMillis();

        /*
         * 成功するまで destination で回してもいいが、そこまでしなくても良さそう。
         */

        final InetSocketAddress destination = selectDestination();
        if (destination == null) {
            // 近接個体がいない場合は無条件成功。
            return new BackupOneResult();
        }

        final CheckOneDemandResult demand = this.checkOneDemandBlockingDriver.execute(new CheckOneDemandOperation(operation.getId(), destination),
                start + timeout - System.currentTimeMillis());
        if (demand == null || demand.isGiveUp()) {
            return BackupOneResult.newGiveUp();
        } else if (demand.isNoDemand()) {
            return new BackupOneResult();
        }

        LOG.log(Level.FINEST, "{0} に {1} の需要があったので複製します。", new Object[] { destination, operation.getId() });

        // return BackupOneResult.newGiveUp();
        final BackupResult result = this.backupBlockingDriver.execute(new BackupOperation(demand.getDemand(), destination),
                start + timeout - System.currentTimeMillis());
        if (result == null || result.isGivenUp()) {
            return BackupOneResult.newGiveUp();
        } else if (result.isSuccess()) {
            return new BackupOneResult();
        } else {
            return BackupOneResult.newFailure();
        }
    }

    private InetSocketAddress selectDestination() {
        final List<AddressedPeer> peers = this.network.getBackupNeighbors(Integer.MAX_VALUE);
        if (peers.isEmpty()) {
            return null;
        } else {
            return peers.get(ThreadLocalRandom.current().nextInt(peers.size())).getPeer();
        }
    }

}
