package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class Backupper extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(Backupper.class.getName());

    // 参照。
    private final InetSocketAddress destination;

    private final NetworkWrapper network;
    private final long interval;
    private final long timeout;

    private final BackupDriverSet drivers;

    private boolean end;

    Backupper(final InetSocketAddress destination, final NetworkWrapper network, final long interval, final long timeout, final BackupDriverSet drivers) {
        super(Level.WARNING);
        if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        } else if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (interval < 0) {
            throw new IllegalArgumentException("Negative interval ( " + interval + " ).");
        } else if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout ( " + timeout + " ).");
        } else if (drivers == null) {
            throw new IllegalArgumentException("Null drivers.");
        }

        this.destination = destination;
        this.network = network;
        this.interval = interval;
        this.timeout = timeout;
        this.drivers = drivers;

        this.end = false;
    }

    private boolean isEnd() {
        /*
         * 終了フラグの検査ともう要らないかどうか検査。
         */
        if (this.end) {
            return true;
        } else {
            if (Thread.currentThread().isInterrupted() || this.network.inBlacklist(this.destination)) {
                this.end = true;
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    protected Void subCall() throws MyRuleException, IOException, InterruptedException {

        firstRecovery();

        while (!isEnd()) {
            final long start = System.currentTimeMillis();

            backup();

            if (isEnd()) {
                break;
            }

            recovery();

            final long sleepTime = start + this.interval - System.currentTimeMillis();
            if (sleepTime > 0) {
                Thread.sleep(sleepTime);
            }
        }

        return null;
    }

    private void firstRecovery() throws IOException, InterruptedException {
        /*
         * 時間無制限でデータ片を取り寄せる。
         */

        while (!isEnd()) {
            final CheckStockResult result = this.drivers.getCheckStockBlocking().execute(new CheckStockOperation(this.destination), this.timeout / 2);// 直接なら短く。

            if (result == null || result.isGivenUp()) {
                LOG.log(Level.FINEST, "{0} への最初の在庫確認が失敗しました。", this.destination);
                // ふて寝。
                Thread.sleep(this.interval / 2);
            } else {
                LOG.log(Level.FINEST, "{0} から最初の在庫が {1} 個報告されました。", new Object[] { this.destination, result.getStockedEntries().size() });
                for (int i = 0; i < result.getStockedEntries().size(); i++) {
                    if (isEnd()) {
                        break;
                    }
                    LOG.log(Level.FINEST, "{0} からの最初の取り寄せ {1} 個目 {2} を始めます。", new Object[] { this.destination, i, result.getStockedEntries().get(i).getId() });
                    this.drivers.getRecoverySelect().execute(new RecoveryOperation(result.getStockedEntries().get(i), this.destination), this.timeout / 2);// 直接なら短く。
                }
                break;
            }
        }
    }

    private void recovery() throws IOException, InterruptedException {
        final long start = System.currentTimeMillis();

        final CheckStockResult result = this.drivers.getCheckStockBlocking().execute(new CheckStockOperation(this.destination), this.timeout / 2);// 直接なら短く。

        if (result == null || result.isGivenUp()) {
            LOG.log(Level.FINEST, "{0} への在庫確認が失敗しました。", this.destination);
            return;
        }

        LOG.log(Level.FINEST, "{0} から在庫が {1} 個報告されました。", new Object[] { this.destination, result.getStockedEntries().size() });

        for (int i = 0; i < result.getStockedEntries().size(); i++) {
            if (isEnd()) {
                break;
            }

            final long currentTimeout = Math.min(this.timeout / 2, start + this.interval - System.currentTimeMillis()); // 直接なら短く。

            if (currentTimeout <= 0) {
                break;
            }

            LOG.log(Level.FINEST, "{0} からの取り寄せ {1} 個目 {2} を始めます。", new Object[] { this.destination, i, result.getStockedEntries().get(i).getId() });
            this.drivers.getRecoverySelect().execute(new RecoveryOperation(result.getStockedEntries().get(i), this.destination), currentTimeout);
        }
    }

    private void backup() throws IOException, InterruptedException {
        final long start = System.currentTimeMillis();

        final CheckDemandResult result = this.drivers.getCheckDemandBlocking().execute(new CheckDemandOperation(this.destination), this.timeout / 2);// 直接なら短く。

        if (result == null || result.isGivenUp()) {
            LOG.log(Level.FINEST, "{0} への発注依頼が失敗しました。", this.destination);
            return;
        }

        LOG.log(Level.FINEST, "{0} から {1} 個の発注が来ました。", new Object[] { this.destination, result.getDemandedEntries().size() });

        for (int i = 0; i < result.getDemandedEntries().size(); i++) {
            if (isEnd()) {
                break;
            }

            final long currentTimeout = Math.min(this.timeout / 2, start + this.interval - System.currentTimeMillis());// 直接なら短く。

            if (currentTimeout <= 0) {
                break;
            }

            LOG.log(Level.FINEST, "{0} への複製 {1} 個目 {2} を始めます。", new Object[] { this.destination, i, result.getDemandedEntries().get(i).getId() });
            this.drivers.getBackupSelect().execute(new BackupOperation(result.getDemandedEntries().get(i), this.destination), currentTimeout);
        }
    }

}
