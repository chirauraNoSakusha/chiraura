/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * @author chirauraNoSakusha
 */
final class BackupperMaster extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(BackupperMaster.class.getName());

    static final class BackupperUnit implements AutoCloseable {
        private final Future<Void> future;

        private BackupperUnit(final Future<Void> future) {
            if (future == null) {
                throw new IllegalArgumentException("Null future.");
            }
            this.future = future;
        }

        private boolean isClosed() throws InterruptedException {
            /*
             * 死んでるかどうか調べる。
             */
            try {
                this.future.get(0, TimeUnit.MILLISECONDS);
            } catch (final ExecutionException e) {
                return true;
            } catch (final TimeoutException e) {
                return false;
            }
            return true;
        }

        @Override
        public void close() {
            this.future.cancel(true);
        }
    }

    // 参照。
    private final NetworkWrapper network;
    private final long interval;

    private final long backupInterval;
    private final long timeout;

    /*
     * このプロセスが死んでも仕事を引き継げるように外部参照するが、
     * backupperPool に触るのはこのプロセスだけと想定する。
     */
    private final Map<AddressedPeer, BackupperUnit> backupperPool;

    private final ExecutorService executor;
    private final BackupDriverSet drivers;

    BackupperMaster(final BlockingQueue<? super Reporter.Report> reportSink, final NetworkWrapper network, final long interval, final long backupInterval,
            final long timeout, final Map<AddressedPeer, BackupperUnit> backupperPool, final ExecutorService executor, final BackupDriverSet drivers) {
        super(reportSink);

        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (interval < 0) {
            throw new IllegalArgumentException("Invalid interval ( " + interval + " ).");
        } else if (backupInterval < 0) {
            throw new IllegalArgumentException("Invalid backup interval ( " + backupInterval + " ).");
        } else if (timeout < 0) {
            throw new IllegalArgumentException("Invalid timeout ( " + timeout + " ).");
        } else if (backupperPool == null) {
            throw new IllegalArgumentException("Null backupper pool.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        } else if (drivers == null) {
            throw new IllegalArgumentException("Null drivers.");
        }
        this.network = network;
        this.interval = interval;
        this.backupInterval = backupInterval;
        this.timeout = timeout;
        this.backupperPool = backupperPool;
        this.executor = executor;
        this.drivers = drivers;
    }

    private static <T> Set<T> getProduct(final Set<T> set1, final Set<T> set2) {
        final Set<T> output = new HashSet<>();
        final Set<T> smaller, larger;
        if (set1.size() <= set2.size()) {
            smaller = set1;
            larger = set2;
        } else {
            smaller = set2;
            larger = set1;
        }
        for (final T elem : smaller) {
            if (larger.contains(elem)) {
                output.add(elem);
            }
        }
        return output;
    }

    /**
     * まともに活動を開始できる状況になるまで待つ。
     * @throws InterruptedException 割り込まれた場合
     */
    private void waitStart() throws InterruptedException {
        if (!this.backupperPool.isEmpty()) {
            // 先人が既に活動を始めていたら待たない。
            LOG.log(Level.FINEST, "先人の仕事を引き継ぎます。");
            return;
        }

        final int div = 10;

        // 最近接個体が安定するのを待つ。
        int stableCount = 0;
        final long start = System.currentTimeMillis();
        List<AddressedPeer> pre = this.network.getBackupNeighbors(1);
        while (!Thread.currentThread().isInterrupted()) {
            Thread.sleep(this.interval / div);

            final List<AddressedPeer> cur = this.network.getBackupNeighbors(1);

            if (!cur.isEmpty() && cur.equals(pre)) {
                stableCount++;
            } else {
                stableCount = 0;
            }
            if (stableCount >= div) {
                break;
            }
            pre = cur;
        }
        LOG.log(Level.FINEST, "{0} ミリ秒で安定しました。", Long.toString(System.currentTimeMillis() - start));
    }

    @Override
    protected Void subCall() throws InterruptedException {
        waitStart();

        while (!Thread.currentThread().isInterrupted()) {
            final Set<AddressedPeer> oldPeers = new HashSet<>(this.backupperPool.keySet());
            final Set<AddressedPeer> newPeers = new HashSet<>(this.network.getBackupNeighbors(Integer.MAX_VALUE));

            final Set<AddressedPeer> product = getProduct(oldPeers, newPeers);
            oldPeers.removeAll(product);
            newPeers.removeAll(product);

            /*
             * oldPeers には、もう近接ではなくなった個体。
             * newPeers には、まだ同期が始められていない個体。
             */

            for (final AddressedPeer peer : oldPeers) {
                final BackupperUnit unit = this.backupperPool.remove(peer);
                unit.close();
                LOG.log(Level.FINEST, "{0} との同期を止めました。", peer);
            }

            for (final AddressedPeer peer : newPeers) {
                final Backupper backupper = new Backupper(peer.getPeer(), this.network, this.backupInterval, this.timeout, this.drivers);
                final Future<Void> future = this.executor.submit(backupper);
                this.backupperPool.put(peer, new BackupperUnit(future));
                LOG.log(Level.FINEST, "{0} との同期を開始しました。", peer);
            }

            // 事故死に対応。
            for (final Map.Entry<AddressedPeer, BackupperUnit> entry : this.backupperPool.entrySet()) {
                if (entry.getValue().isClosed()) {
                    final Backupper backupper = new Backupper(entry.getKey().getPeer(), this.network, this.backupInterval, this.timeout,
                            this.drivers);
                    final Future<Void> future = this.executor.submit(backupper);
                    this.backupperPool.put(entry.getKey(), new BackupperUnit(future));
                    LOG.log(Level.FINEST, "{0} との同期を再開しました。", entry.getKey());
                }
            }

            // 定期待機。
            Thread.sleep(this.interval);
        }

        return null;
    }

}
