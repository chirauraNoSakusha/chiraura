package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;
import java.util.Map;

import nippon.kawauso.chiraura.lib.container.BasicInputOrderedMap;
import nippon.kawauso.chiraura.lib.container.InputOrderedMap;

/**
 * 一律な有効期限のある除外対象個体一覧。
 * 時間経過で勝手に消えていくので、一貫性は無い。
 * @author chirauraNoSakusha
 */
final class TimeLimitedPeerBlacklist implements PeerBlacklist {

    private final int capacity;
    private final long timeout;
    private final InputOrderedMap<InetSocketAddress, Long> peerToLimit;

    TimeLimitedPeerBlacklist(final int capacity, final long timeout) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Negative capacity ( " + capacity + " ).");
        } else if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout ( " + timeout + " ).");
        }

        this.capacity = capacity;
        this.timeout = timeout;
        this.peerToLimit = new BasicInputOrderedMap<>();
    }

    @Override
    public synchronized boolean contains(final InetSocketAddress peer) {
        final Long limit = this.peerToLimit.get(peer);
        if (limit == null) {
            return false;
        } else if (limit <= System.currentTimeMillis()) {
            // 有効期限切れ。
            this.peerToLimit.remove(peer);
            return false;
        } else {
            return true;
        }
    }

    private void removeTimeout() {
        final long current = System.currentTimeMillis();
        while (true) {
            final Map.Entry<InetSocketAddress, Long> entry = this.peerToLimit.getEldest();
            if (entry == null || current <= entry.getValue()) {
                break;
            } else {
                this.peerToLimit.removeEldest();
            }
        }
    }

    @Override
    public synchronized boolean add(final InetSocketAddress peer) {
        final Long oldLimit = this.peerToLimit.put(peer, System.currentTimeMillis() + this.timeout);
        while (this.peerToLimit.size() > this.capacity) {
            this.peerToLimit.removeEldest();
        }
        removeTimeout();
        return oldLimit == null && this.peerToLimit.containsKey(peer);
    }

    @Override
    public synchronized boolean remove(final InetSocketAddress peer) {
        removeTimeout();
        return this.peerToLimit.remove(peer) != null;
    }

}
