/**
 * 
 */
package nippon.kawauso.chiraura.storage;

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
final class LockPool<T> {

    private final ConcurrentMap<T, ReentrantLock> locks;

    /*
     * インスタンスを locks から remove() するのは、
     * そのインスタンスをロックしているプロセスのみ。
     */

    /**
     * 作成する。
     */
    LockPool() {
        this.locks = new ConcurrentHashMap<>();
    }

    /**
     * ロックする。
     * ロックできるまで待機する。
     * @param key ロックの鍵
     * @throws InterruptedException 待機中にインタラプトされた場合
     */
    void lock(final T key) throws InterruptedException {
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
                break;
            } else {
                lock.unlock();
            }
        }
    }

    /**
     * ロックする。
     * ロックできないときは諦める
     * @param key ロックの鍵
     * @return ロックできたら true
     */
    boolean tryLock(final T key) {
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
    boolean tryLock(final T key, final long timeout) throws InterruptedException {
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
    void unlock(final T key) {
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
    }

    /**
     * 空かどうか検査する。
     * @return 空の場合のみ true
     */
    boolean isEmpty() {
        return this.locks.isEmpty();
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append("[numOfLocks=").append(this.locks.size())
                .append(']').toString();
    }

}
