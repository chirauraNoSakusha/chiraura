/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ブロッキングキューの集合。
 * @author chirauraNoSakusha
 */
final class BlockingQueuePool<K, E> {

    private final Lock lock;
    private final Map<K, LockQueue<E>> queues;

    BlockingQueuePool() {
        this.lock = new ReentrantLock();
        this.queues = new HashMap<>();
    }

    private static <E> LockQueue<E> newLockQueue() {
        return new BasicLockQueue<>(new LinkedList<E>());
    }

    /**
     * キューに要素を入れる。
     * @param key キューの識別子
     * @param element 要素
     * @return 新しくキューが作られた場合は true
     */
    boolean put(final K key, final E element) {
        LockQueue<E> queue = null;
        boolean created = false;

        try { // キューのロック。
            this.lock.lock();
            try { // プールのロック。
                queue = this.queues.get(key);
                if (queue == null) {
                    queue = newLockQueue();
                    this.queues.put(key, queue);
                    created = true;
                }
                // ロック結合。
                queue.lock();
            } finally {
                this.lock.unlock();
            }

            queue.put(element);
        } finally {
            if (queue != null) {
                queue.unlock();
            }
        }

        return created;
    }

    /**
     * キューに入っている要素を取り出す。
     * 要素が入っていない場合は待つ。
     * @param key キューの識別子
     * @return 先頭の要素
     * @throws InterruptedException 割り込まれた場合
     */
    E take(final K key) throws InterruptedException {
        LockQueue<E> queue = null;

        try { // キューのロック。
            this.lock.lock();
            try { // プールのロック。
                queue = this.queues.get(key);
                if (queue == null) {
                    throw new IllegalArgumentException("No queue for " + key);
                }
                // ロック結合。
                queue.lock();
            } finally {
                this.lock.unlock();
            }

            return queue.take();
        } finally {
            if (queue != null) {
                queue.unlock();
            }
        }
    }

    /**
     * キューに入っている要素を取り出す。
     * 要素が入っていない場合は制限時間まで待つ。
     * @param key キューの識別子
     * @param waitMilliSeconds 制限時間 (ミリ秒)
     * @return 先頭の要素。
     *         制限時間切れの場合は null
     * @throws InterruptedException 割り込まれた場合
     */
    E take(final K key, final long waitMilliSeconds) throws InterruptedException {
        LockQueue<E> queue = null;

        try { // キューのロック。
            this.lock.lock();
            try { // プールのロック。
                queue = this.queues.get(key);
                if (queue == null) {
                    throw new IllegalArgumentException("No queue for " + key);
                }
                // ロック結合。
                queue.lock();
            } finally {
                this.lock.unlock();
            }

            return queue.take(waitMilliSeconds);
        } finally {
            if (queue != null) {
                queue.unlock();
            }
        }
    }

    /**
     * キューに入っている要素を取り出す。
     * @param key キューの識別子
     * @return 先頭の要素。
     *         要素が入っていないときは null
     */
    E takeIfExists(final K key) {
        LockQueue<E> queue = null;

        try { // キューのロック。
            this.lock.lock();
            try { // プールのロック。
                queue = this.queues.get(key);
                if (queue == null) {
                    throw new IllegalArgumentException("No queue for " + key);
                }
                // ロック結合。
                queue.lock();
            } finally {
                this.lock.unlock();
            }

            return queue.takeIfExists();
        } finally {
            if (queue != null) {
                queue.unlock();
            }
        }
    }

    /**
     * キューを追加する。
     * @param key キューの識別子
     * @return 新しくキューが作られた場合、つまり、まだ無かった場合は true
     */
    boolean addQueue(final K key) {
        this.lock.lock();
        try { // プールのロック。
            if (this.queues.containsKey(key)) {
                return false;
            } else {
                final LockQueue<E> queue = newLockQueue();
                this.queues.put(key, queue);
                return true;
            }
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * キューを削除する
     * @param key キューの識別子
     * @return キューが削除された場合、つまり、有った場合は残存要素。
     *         キューが削除されなかった場合、つまり、無かった場合は null
     */
    List<E> removeQueue(final K key) {
        LockQueue<E> queue = null;

        try { // キューのロック。
            this.lock.lock();
            try { // プールのロック。
                queue = this.queues.remove(key);
                if (queue == null) {
                    return null;
                } else {
                    // ロック結合。
                    queue.lock();
                }
            } finally {
                this.lock.unlock();
            }

            return queue.removeAll();
        } finally {
            if (queue != null) {
                queue.unlock();
            }
        }
    }

    /**
     * キューに入っている要素の数を返す。
     * @param key キューの識別子
     * @return キューに入っている要素の数。
     *         対応するキューが無い場合は負値
     */
    int size(final K key) {
        LockQueue<E> queue = null;

        try { // キューのロック。
            this.lock.lock();
            try { // プールのロック。
                queue = this.queues.get(key);
                if (queue == null) {
                    return -1;
                }
                // ロック結合。
                queue.lock();
            } finally {
                this.lock.unlock();
            }

            return queue.size();
        } finally {
            if (queue != null) {
                queue.unlock();
            }
        }
    }

    /**
     * キューの存在検査。
     * @param key キューの識別子
     * @return 有れば true
     */
    public boolean conteinsQueue(final K key) {
        this.lock.lock();
        try { // プールのロック。
            return this.queues.containsKey(key);
        } finally {
            this.lock.unlock();
        }
    }

}
