package nippon.kawauso.chiraura.messenger;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ロック結合して使うキューの実装。
 * 使う前後で lock, unlock すること。
 * @author chirauraNoSakusha
 */
final class BasicLockQueue<E> implements LockQueue<E> {

    private final Lock lock;
    private final Condition notEmpty;
    private final Queue<E> queue;

    /**
     * 初期化。
     * @param queue 使用するキュー
     */
    BasicLockQueue(final Queue<E> queue) {
        if (queue == null) {
            throw new IllegalArgumentException("Null queue.");
        }

        this.lock = new ReentrantLock();
        this.notEmpty = this.lock.newCondition();
        this.queue = queue;
    }

    @Override
    public void lock() {
        this.lock.lock();
    }

    @Override
    public void unlock() {
        this.lock.unlock();
    }

    @Override
    public void put(final E element) {
        this.queue.offer(element);
        this.notEmpty.signal();
    }

    @Override
    public E take() throws InterruptedException {
        while (this.queue.isEmpty()) {
            this.notEmpty.await();
        }
        return this.queue.poll();
    }

    @Override
    public E take(final long waitMilliSeconds) throws InterruptedException {
        long waitNanoSeconds = TimeUnit.MILLISECONDS.toNanos(waitMilliSeconds);
        while (this.queue.isEmpty()) {
            if (waitNanoSeconds > 0) {
                waitNanoSeconds = this.notEmpty.awaitNanos(waitNanoSeconds);
            } else {
                return null;
            }
        }
        return this.queue.poll();
    }

    @Override
    public E takeIfExists() {
        return this.queue.poll();
    }

    @Override
    public List<E> removeAll() {
        final List<E> remains = new ArrayList<>(this.queue.size());
        for (final E element : this.queue) {
            remains.add(element);
        }
        return remains;
    }

    @Override
    public int size() {
        return this.queue.size();
    }

}
