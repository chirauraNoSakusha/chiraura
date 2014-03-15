package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.container.BasicInputOrderedMap;
import nippon.kawauso.chiraura.lib.container.InputOrderedMap;

/**
 * 先入れ先出しの掃き溜め。
 * 並列対応。
 * @author chirauraNoSakusha
 */
final class FifoPeerPot implements PeerPot {

    private final int capacity;

    /*
     * InputOrderedSet は作ってないので。
     */
    private final InputOrderedMap<InetSocketAddress, InetSocketAddress> peers;

    FifoPeerPot(final int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Negative capacity ( " + capacity + " ).");
        }

        this.capacity = capacity;
        this.peers = new BasicInputOrderedMap<>();
    }

    @Override
    public synchronized InetSocketAddress get() {
        if (this.peers.isEmpty()) {
            return null;
        }
        final InetSocketAddress peer = this.peers.removeEldest().getKey();
        this.peers.put(peer, peer);
        return peer;
    }

    @Override
    public synchronized void put(final InetSocketAddress peer) {
        if (!this.peers.containsKey(peer)) {
            // 先入れを保つため。
            this.peers.put(peer, peer);
        }
        while (this.peers.size() > this.capacity) {
            this.peers.removeEldest();
        }
    }

    @Override
    public synchronized List<InetSocketAddress> getAll() {
        return new ArrayList<>(this.peers.keySet());
    }

}
