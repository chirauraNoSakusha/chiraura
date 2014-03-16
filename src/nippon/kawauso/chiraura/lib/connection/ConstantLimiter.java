package nippon.kawauso.chiraura.lib.connection;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;

/**
 * 一定期間の一定量・一定回数で制限。
 * @author chirauraNoSakusha
 */
abstract class ConstantLimiter<T> implements Limiter<T> {

    private static final Logger LOG = Logger.getLogger(ConstantLimiter.class.getName());

    // データをまとめるためだけ。
    private static final class DateAndValue {
        private final long date;
        private final long value;

        private DateAndValue(final long date, final long value) {
            this.date = date;
            this.value = value;
        }

        @Override
        public String toString() {
            return (new StringBuilder("[")).append(LoggingFunctions.getSimpleDate(this.date)).append(", ").append(this.value).append(']').toString();
        }
    }

    private static final class Sum {
        private final Lock lock;
        private long sum;
        private final Deque<DateAndValue> entries;

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
            // 回数制限だけのときは value == 0 で使うこともあるので、
            // return value == 0 では駄目。
            return this.entries.isEmpty();
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
            this.entries.offerLast(new DateAndValue(date, size));
            this.sum += size;
        }

        // 時刻が古いものを削除。
        private void trim(final long deadline) {
            final DateAndValue latest = this.entries.peekLast();
            if (latest == null) {
                return;
            } else if (latest.date <= deadline) {
                // 全部期限切れ。
                this.entries.clear();
                this.sum = 0;
                return;
            }

            while (true) {
                final DateAndValue oldest = this.entries.peekFirst();
                if (oldest == null || deadline < oldest.date) {
                    break;
                }
                this.entries.pollFirst();
                this.sum -= oldest.value;
            }
        }

        @Override
        public String toString() {
            return (new StringBuilder(ConstantLimiter.class.getSimpleName())).append('.').append(this.getClass().getSimpleName())
                    .append('[').append(this.sum)
                    .append(", ").append(this.entries)
                    .append(']').toString();
        }

    }

    // 参照。
    private final long duration;
    private final long valueLimit;
    private final int countLimit;
    private final long penalty;

    // 保持。
    private final Lock lock;
    private final Map<T, Sum> sums;

    ConstantLimiter(final long duration, final long valueLimit, final int countLimit, final long penalty) {
        if (duration < 0) {
            throw new IllegalArgumentException("Negative duration ( " + duration + " ).");
        } else if (valueLimit < 0) {
            throw new IllegalArgumentException("Negative value limit ( " + valueLimit + " ).");
        } else if (countLimit < 0) {
            throw new IllegalArgumentException("Negative count limit ( " + countLimit + " ).");
        } else if (penalty < 0) {
            throw new IllegalArgumentException("Negative penalty ( " + penalty + " ).");
        }

        this.duration = duration;
        this.valueLimit = valueLimit;
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

    @Override
    public long addValueAndCheckPenalty(final T key, final long value) throws InterruptedException {
        long curPenalty = 0L;

        Sum sum = null;
        try { // 個別のロック。
            this.lock.lockInterruptibly();
            try { // 全体のロック。
                sum = this.sums.get(key);
                if (sum == null) {
                    sum = new Sum();
                    this.sums.put(key, sum);
                }

                // ロック結合。
                sum.lock();
            } finally {
                this.lock.unlock();
            }

            // 発生した時刻とは違うけど大した問題にはならないはず。
            // 一応、ロック後に取得しているので、sum.entries 内では確実に時刻順で並ぶ。
            final long cur = System.currentTimeMillis();
            sum.trim(cur - this.duration);
            sum.add(cur, value);
            if (this.valueLimit < sum.getSize() || this.countLimit < sum.getCount()) {
                curPenalty = sum.getFirstDate() + this.duration - cur // 一番古いのが削除されるまでの時間。
                        + this.penalty;
            }
        } finally {
            if (sum != null) {
                sum.unlock();
            }
        }

        return curPenalty;
    }

    @Override
    public long checkPenalty(final T key) throws InterruptedException {
        long curPenalty = 0L;
        boolean empty = false;

        Sum sum = null;
        try { // 個別のロック。
            this.lock.lockInterruptibly();
            try { // 全体のロック。
                sum = this.sums.get(key);
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
            } else if (this.valueLimit < sum.getSize() || this.countLimit < sum.getCount()) {
                curPenalty = sum.getFirstDate() + this.duration - cur // 一番古いのが削除されるまでの時間。
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
                sum = this.sums.get(key);
                if (sum != null) {
                    sum.lock();
                    try {
                        if (sum.isEmpty()) {
                            this.sums.remove(key);
                        }
                    } finally {
                        sum.unlock();
                    }
                }
            } finally {
                this.lock.unlock();
            }
        }

        return curPenalty;
    }

    @Override
    public int checkCount(final T key) throws InterruptedException {
        int curCount = 0;

        Sum sum = null;
        try { // 個別のロック。
            this.lock.lockInterruptibly();
            try { // 全体のロック。
                sum = this.sums.get(key);
                if (sum == null) {
                    return 0;
                }

                // ロック結合。
                sum.lock();
            } finally {
                this.lock.unlock();
            }

            final long cur = System.currentTimeMillis();
            sum.trim(cur - this.duration);
            curCount = sum.getCount();
        } finally {
            if (sum != null) {
                sum.unlock();
            }
        }

        if (curCount == 0) {
            // 空なので削除。
            this.lock.lockInterruptibly();
            try {
                sum = this.sums.get(key);
                if (sum != null) {
                    sum.lock();
                    try {
                        if (sum.isEmpty()) {
                            this.sums.remove(key);
                        }
                    } finally {
                        sum.unlock();
                    }
                }
            } finally {
                this.lock.unlock();
            }
        }

        return curCount;
    }

    @Override
    public boolean remove(final T key) throws InterruptedException {
        long curPenalty = 0L;

        Sum sum = null;
        try { // 個別のロック。
            this.lock.lockInterruptibly();
            try { // 全体のロック。
                sum = this.sums.get(key);
                if (sum == null) {
                    // 解放済み。
                    return true;
                }

                // ロック結合。
                sum.lock();
            } finally {
                this.lock.unlock();
            }

            final long cur = System.currentTimeMillis();
            sum.trim(cur - this.duration);
            if (!sum.isEmpty()) {
                curPenalty = sum.getLastDate() + this.duration - cur; // 今あるのが全部削除されるまでの時間。
            }
        } finally {
            if (sum != null) {
                sum.unlock();
            }
        }

        if (curPenalty > 0) {
            LOG.log(Level.FINEST, "削除待ちで {0} ミリ秒さぼります。", curPenalty);
            Thread.sleep(curPenalty);
        }

        this.lock.lockInterruptibly();
        try {
            sum = this.sums.get(key);
            if (sum != null) {
                sum.lock();
                try {
                    if (sum.isEmpty() || sum.getLastDate() <= System.currentTimeMillis()) {
                        this.sums.remove(key);
                        return true;
                    }
                } finally {
                    sum.unlock();
                }
            }
        } finally {
            this.lock.unlock();
        }

        return false;
    }

}
