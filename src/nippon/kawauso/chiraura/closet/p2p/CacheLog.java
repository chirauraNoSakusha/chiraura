package nippon.kawauso.chiraura.closet.p2p;

import java.util.Map;

import nippon.kawauso.chiraura.lib.container.BasicInputOrderedMap;
import nippon.kawauso.chiraura.lib.container.InputOrderedMap;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class CacheLog {

    static final class Entry {

        private final boolean notFound;
        private final long accessDate;

        private Entry(final boolean notFound, final long accessDate) {
            this.notFound = notFound;
            this.accessDate = accessDate;
        }

        boolean isNotFound() {
            return this.notFound;
        }

        long getAccessDate() {
            return this.accessDate;
        }

    }

    private final int limit;
    private final long duration;
    private final InputOrderedMap<Chunk.Id<?>, Entry> entries;

    CacheLog(final int limit, final long duration) {
        if (limit < 0) {
            throw new IllegalArgumentException("Negative limit ( " + limit + " ).");
        } else if (duration < 0) {
            throw new IllegalArgumentException("Negative duration ( " + duration + " ).");
        }
        this.limit = limit;
        this.duration = duration;
        this.entries = new BasicInputOrderedMap<>();
    }

    synchronized boolean contains(final Chunk.Id<?> id) {
        return this.entries.containsKey(id);
    }

    private void trim(final long cur) {
        while (this.limit < this.entries.size()) {
            this.entries.removeEldest();
        }
        while (!this.entries.isEmpty()) {
            final Map.Entry<Chunk.Id<?>, Entry> eldest = this.entries.getEldest();
            if (eldest.getValue().getAccessDate() + this.duration < cur) {
                this.entries.remove(eldest.getKey());
            } else {
                break;
            }
        }
    }

    synchronized Entry get(final Chunk.Id<?> id) {
        trim(System.currentTimeMillis());
        return this.entries.get(id);
    }

    synchronized void add(final Chunk.Id<?> id, final long accessDate) {
        final long cur = System.currentTimeMillis();
        trim(cur);
        if (accessDate + this.duration < cur) {
            // 古過ぎ。
        } else if (cur < accessDate) {
            // 未来設定は NG。
            this.entries.put(id, new Entry(false, cur));
        } else {
            this.entries.put(id, new Entry(false, accessDate));
        }
    }

    void addNotFound(final Chunk.Id<?> id, final long accessDate) {
        final long cur = System.currentTimeMillis();
        if (accessDate + this.duration < cur) {
            // 古過ぎ。
        } else if (cur < accessDate) {
            // 未来設定は NG。
            this.entries.put(id, new Entry(true, cur));
        } else {
            this.entries.put(id, new Entry(true, accessDate));
        }
    }

}
