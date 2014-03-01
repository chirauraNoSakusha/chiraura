/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 一定期間の一定量・一定回数で制限。
 * @author chirauraNoSakusha
 */
abstract class ConstantTrafficLimiter<T> {

    private static final Logger LOG = Logger.getLogger(ConstantTrafficLimiter.class.getName());

    // データをまとめるためだけ。
    private static final class DateAndSize {
        private final long date;
        private final long size;

        private DateAndSize(final long date, final long size) {
            this.date = date;
            this.size = size;
        }
    }

    private static final class Sum {
        private final Lock lock;
        private long sum;
        private final Deque<DateAndSize> entries;

        private Sum() {
            this.lock = new ReentrantLock();
            this.sum = 0;
            this.entries = new LinkedList<>();
        }

        private void lock() throws InterruptedException {
            this.lock.lockInterruptibly();
        }

        private void unlock() {
            this.lock.unlock();
        }

        private boolean isEmpty() {
            return this.sum == 0;
        }

        private long getSize() {
            return this.sum;
        }

        private int getCount() {
            return this.entries.size();
        }

        private long getFirstDate() {
            return this.entries.peekFirst().date;
        }

        private long getLastDate() {
            return this.entries.peekLast().date;
        }

        // 末尾に追加。
        private void add(final long date, final long size) {
            this.entries.offerLast(new DateAndSize(date, size));
            this.sum += size;
        }

        // 時刻が古いものを削除。
        private void trim(final long deadline) {
            final DateAndSize latest = this.entries.peekLast();
            if (latest == null) {
                return;
            } else if (latest.date <= deadline) {
                // 全部期限切れ。
                this.entries.clear();
                this.sum = 0;
                return;
            }

            while (true) {
                final DateAndSize oldest = this.entries.peekFirst();
                if (oldest == null || deadline < oldest.date) {
                    break;
                }
                this.entries.pollFirst();
                this.sum -= oldest.size;
            }
        }
    }

    // 参照。
    private final long duration;
    private final long sizeLimit;
    private final int countLimit;
    private final long penalty;

    // 保持。
    private final Lock lock;
    private final Map<T, Sum> sums;

    ConstantTrafficLimiter(final long duration, final long sizeLimit, final int countLimit, final long penalty) {
        if (duration < 0) {
            throw new IllegalArgumentException("Negative duration ( " + sizeLimit + " ).");
        } else if (sizeLimit < 0) {
            throw new IllegalArgumentException("Negative size limit ( " + sizeLimit + " ).");
        } else if (countLimit < 0) {
            throw new IllegalArgumentException("Negative count limit ( " + countLimit + " ).");
        } else if (penalty < 0) {
            throw new IllegalArgumentException("Negative penalty ( " + penalty + " ).");
        }

        this.duration = duration;
        this.sizeLimit = sizeLimit;
        this.countLimit = countLimit;
        this.penalty = penalty;

        this.lock = new ReentrantLock();
        this.sums = new HashMap<>();
    }

    boolean isEmpty() {
        this.lock.lock();
        try {
            return this.sums.isEmpty();
        } finally {
            this.lock.unlock();
        }
    }

    long nextSleep(final long size, final T destination) throws InterruptedException {
        long sleep = 0L;

        Sum sum = null;
        try { // 個別のロック。
            this.lock.lockInterruptibly();
            try { // 全体のロック。
                sum = this.sums.get(destination);
                if (sum == null) {
                    sum = new Sum();
                    this.sums.put(destination, sum);
                }

                // ロック結合。
                sum.lock();
            } finally {
                this.lock.unlock();
            }

            // 通信した時刻とは違うけど大した問題にはならないはず。
            // 一応、ロック後に取得しているので、sum.entries 内では確実に時刻順で並ぶ。
            final long cur = System.currentTimeMillis();
            sum.trim(cur - this.duration);
            sum.add(cur, size);
            if (this.sizeLimit <= sum.getSize() || this.countLimit <= sum.getCount()) {
                sleep = sum.getFirstDate() + this.duration - cur // 一番古いのが削除されるまでの時間。
                        + this.penalty;
            }
        } finally {
            if (sum != null) {
                sum.unlock();
            }
        }

        return sleep;
    }

    long nextSleep(final T destination) throws InterruptedException {
        long sleep = 0L;
        boolean empty = false;

        Sum sum = null;
        try { // 個別のロック。
            this.lock.lockInterruptibly();
            try { // 全体のロック。
                sum = this.sums.get(destination);
                if (sum == null) {
                    return 0L;
                }

                // ロック結合。
                sum.lock();
            } finally {
                this.lock.unlock();
            }

            final long cur = System.currentTimeMillis();
            sum.trim(cur - this.duration);
            if (sum.isEmpty()) {
                empty = true;
            } else if (this.sizeLimit <= sum.getSize() || this.countLimit <= sum.getCount()) {
                sleep = sum.getFirstDate() + this.duration - cur // 一番古いのが削除されるまでの時間。
                        + this.penalty;
            }
        } finally {
            if (sum != null) {
                sum.unlock();
            }
        }

        if (empty) {
            // 空なので削除。
            this.lock.lockInterruptibly();
            try {
                sum = this.sums.get(destination);
                if (sum != null) {
                    sum.lock();
                    try {
                        if (sum.isEmpty()) {
                            this.sums.remove(destination);
                        }
                    } finally {
                        sum.unlock();
                    }
                }
            } finally {
                this.lock.unlock();
            }
        }

        return sleep;
    }

    void remove(final T destination) throws InterruptedException {
        long sleep = 0L;

        Sum sum = null;
        try { // 個別のロック。
            this.lock.lockInterruptibly();
            try { // 全体のロック。
                sum = this.sums.get(destination);
                if (sum == null) {
                    // 解放済み。
                    return;
                }

                // ロック結合。
                sum.lock();
            } finally {
                this.lock.unlock();
            }

            final long cur = System.currentTimeMillis();
            sum.trim(cur - this.duration);
            if (!sum.isEmpty()) {
                sleep = sum.getLastDate() + this.duration - cur; // 今あるのが全部削除されるまでの時間。
            }
        } finally {
            if (sum != null) {
                sum.unlock();
            }
        }

        if (sleep > 0) {
            LOG.log(Level.FINEST, "削除待ちで {0} ミリ秒さぼります。", Long.toString(sleep));
            Thread.sleep(sleep);
        }

        this.lock.lockInterruptibly();
        try {
            sum = this.sums.get(destination);
            if (sum != null) {
                sum.lock();
                try {
                    if (sum.isEmpty() || sum.getLastDate() <= System.currentTimeMillis()) {
                        this.sums.remove(destination);
                    }
                } finally {
                    sum.unlock();
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

}
