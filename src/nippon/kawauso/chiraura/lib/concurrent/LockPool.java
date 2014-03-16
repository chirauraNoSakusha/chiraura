package nippon.kawauso.chiraura.lib.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ロックをまとめて管理する。
 * ロックには再入性がある。
 * @param <T> 鍵のクラス
 * @author chirauraNoSakusha
 */
public final class LockPool<T> {

    // private static final Logger LOG = Logger.getLogger(LockPool.class.getName());

    private final ConcurrentMap<T, ReentrantLock> locks;

    /*
     * インスタンスを locks から remove() するのは、
     * そのインスタンスをロックしているプロセスのみ。
     */

    /**
     * 作成する。
     */
    public LockPool() {
        this.locks = new ConcurrentHashMap<>();
    }

    /**
     * ロックする。
     * ロックできるまで待機する。
     * @param key ロックの鍵
     * @throws InterruptedException 待機中にインタラプトされた場合
     */
    public void lock(final T key) throws InterruptedException {
        // LOG.log(Level.SEVERE, "lock in " + key);
        while (true) {
            ReentrantLock lock = this.locks.get(key);
            if (lock == null) {
                final ReentrantLock newLock = new ReentrantLock();
                lock = this.locks.putIfAbsent(key, newLock);
                if (lock == null) {
                    lock = newLock;
                }
            }

            /*
             * lock をロックする前に、lock をロックしている別プロセスが lock を locks から削除し得る。
             * ロックした後であれば、別プロセスが lock を locks から削除することはない。
             * そこで、ロックした後で、 lock が locks にあるか確認する。
             */
            lock.lockInterruptibly();

            if (lock == this.locks.get(key)) {
                // LOG.log(Level.SEVERE, "lock success " + key);
                break;
            } else {
                lock.unlock();
                // LOG.log(Level.SEVERE, "lock failure " + key);
            }
        }
    }

    /**
     * ロックする。
     * ロックできないときは諦める
     * @param key ロックの鍵
     * @return ロックできたら true
     */
    public boolean tryLock(final T key) {
        while (true) {
            ReentrantLock lock = this.locks.get(key);
            if (lock == null) {
                final ReentrantLock newLock = new ReentrantLock();
                lock = this.locks.putIfAbsent(key, newLock);
                if (lock == null) {
                    lock = newLock;
                }
            }

            /*
             * lock をロックする前に、lock をロックしている別プロセスが lock を locks から削除し得る。
             * ロックした後であれば、別プロセスが lock を locks から削除することはない。
             * そこで、ロックした後で、 lock が locks にあるか確認する。
             */
            if (lock.tryLock()) {
                if (lock == this.locks.get(key)) {
                    return true;
                } else {
                    lock.unlock();
                }
            } else {
                return false;
            }
        }
    }

    /**
     * ロックする。
     * 制限時間の間はロックできるまで待機する。
     * @param key ロックの鍵
     * @param timeout 制限時間
     * @return ロックできたら true。
     *         false なら時間切れ
     * @throws InterruptedException 待機中にインタラプトされた場合
     */
    public boolean tryLock(final T key, final long timeout) throws InterruptedException {
        while (true) {
            ReentrantLock lock = this.locks.get(key);
            if (lock == null) {
                final ReentrantLock newLock = new ReentrantLock();
                lock = this.locks.putIfAbsent(key, newLock);
                if (lock == null) {
                    lock = newLock;
                }
            }

            /*
             * lock をロックする前に、lock をロックしている別プロセスが lock を locks から削除し得る。
             * ロックした後であれば、別プロセスが lock を locks から削除することはない。
             * そこで、ロックした後で、 lock が locks にあるか確認する。
             */
            if (lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
                if (lock == this.locks.get(key)) {
                    return true;
                } else {
                    lock.unlock();
                }
            } else {
                return false;
            }
        }
    }

    /**
     * 解放する。
     * @param key 解放する鍵
     * @throws IllegalArgumentException key によるロックがされていない場合
     */
    public void unlock(final T key) {
        // LOG.log(Level.SEVERE, "unlock in " + key);

        final ReentrantLock lock = this.locks.get(key);
        if (lock == null) {
            throw new IllegalMonitorStateException("Not locked ( " + key + " ).");
        }
        if (lock.getHoldCount() == 1 && !lock.hasQueuedThreads()) {
            // ロック待ちのプロセスも無いし、次の unlock() でこのロックを手放す。
            this.locks.remove(key);
        }

        /*
         * lock を locks から削除していても、既に lock への参照を持った別プロセスが lock をロックしようとし得る。
         * その対処は lock() 内で行う。
         */

        lock.unlock();
        // LOG.log(Level.SEVERE, "unlock success " + key);
    }

    /**
     * 空かどうか検査する。
     * @return 空の場合のみ true
     */
    public boolean isEmpty() {
        return this.locks.isEmpty();
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append("[numOfLocks=").append(this.locks.size())
                .append(']').toString();
    }

}
