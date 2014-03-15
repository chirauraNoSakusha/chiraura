package nippon.kawauso.chiraura.network;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.container.BasicInputOrderedNavigableMap;
import nippon.kawauso.chiraura.lib.container.InputOrderedNavigableMap;
import nippon.kawauso.chiraura.lib.container.Pair;

/**
 * 1 つの個体から見える CostomChord の構造。
 * 並列対応。
 * @author chirauraNoSakusha
 */
final class BasicCcView implements CcView {

    private static final class PeerCache {

        private static final class Entry<K, V> implements Map.Entry<K, V> {
            private final K key;
            private final V value;

            private Entry(final K key, final V value) {
                this.key = key;
                this.value = value;
            }

            @Override
            public K getKey() {
                return this.key;
            }

            @Override
            public V getValue() {
                return this.value;
            }

            @Override
            public V setValue(final V value) {
                throw new UnsupportedOperationException("Not supported.");
            }

        }

        private final InputOrderedNavigableMap<Address, AddressedPeer> distanceToPeer;
        private final Map<InetSocketAddress, Address> peerToDistance;

        private PeerCache() {
            this.distanceToPeer = new BasicInputOrderedNavigableMap<>();
            this.peerToDistance = new HashMap<>();
        }

        @SuppressWarnings("unused")
        private boolean isEmpty() {
            return this.peerToDistance.isEmpty();
        }

        private int size() {
            return this.peerToDistance.size();
        }

        private AddressedPeer get(final Address distacne) {
            return this.distanceToPeer.get(distacne);
        }

        private Address getDistance(final InetSocketAddress peer) {
            return this.peerToDistance.get(peer);
        }

        @SuppressWarnings("unused")
        private Map.Entry<Address, AddressedPeer> get(final InetSocketAddress peer) {
            final Address distance = this.peerToDistance.get(peer);
            if (distance == null) {
                return null;
            } else {
                return new Entry<>(distance, this.distanceToPeer.get(distance));
            }
        }

        private Map.Entry<Address, AddressedPeer> getLargest() {
            return this.distanceToPeer.lastEntry();
        }

        /**
         * 距離が targetDistance 未満で最大の個体を返す。
         * @param targetDistance 始点
         * @return 距離が targetDistance 未満で最大の個体。
         *         無い場合は null
         */
        private Map.Entry<Address, AddressedPeer> searchSmaller(final Address targetDistance) {
            return this.distanceToPeer.lowerEntry(targetDistance);
        }

        /**
         * 距離が targetDistance 以下で最大の個体を返す。
         * @param targetDistance 始点
         * @return 距離が targetDistance 以下で最大の個体。
         *         無い場合は null
         */
        private Map.Entry<Address, AddressedPeer> searchSmallerOrEquals(final Address targetDistance) {
            return this.distanceToPeer.floorEntry(targetDistance);
        }

        /**
         * 距離が targetDistance 超で最小の個体を返す。
         * @param targetDistance 始点
         * @return 距離が targetDistance 超で最小の個体。
         *         無い場合は null
         */
        private Map.Entry<Address, AddressedPeer> searchLarger(final Address targetDistance) {
            return this.distanceToPeer.higherEntry(targetDistance);
        }

        private List<AddressedPeer> getAll() {
            return new ArrayList<>(this.distanceToPeer.values());
        }

        private AddressedPeer put(final Address distance, final AddressedPeer peer) {
            this.peerToDistance.put(peer.getPeer(), distance);
            return this.distanceToPeer.put(distance, peer);
        }

        private Map.Entry<Address, AddressedPeer> remove(final InetSocketAddress peer) {
            final Address distance = this.peerToDistance.remove(peer);
            if (distance == null) {
                return null;
            }
            return new Entry<>(distance, this.distanceToPeer.remove(distance));
        }

        private Map.Entry<Address, AddressedPeer> removeEldest() {
            final Map.Entry<Address, AddressedPeer> entry = this.distanceToPeer.removeEldest();
            if (entry != null) {
                this.peerToDistance.remove(entry.getValue().getPeer());
            }
            return entry;
        }

    }

    private final Address base;
    private final CcShortcutTable fingers;
    private final CcNeighborList successors;
    private final CcNeighborList predecessors;

    private final int capacity;
    private final PeerCache peerCache;

    BasicCcView(final Address base, final int capacity) {
        if (base == null) {
            throw new IllegalArgumentException("Null base.");
        } else if (capacity < 0) {
            throw new IllegalArgumentException("Invalid capacity.");
        }

        this.base = base;
        this.fingers = new BasicCcShortcutTable();
        this.successors = new CcSuccessorList();
        this.predecessors = new CcPredecessorList();
        this.capacity = capacity;
        this.peerCache = new PeerCache();
    }

    @Override
    public Address getBase() {
        return this.base;
    }

    @Override
    public Pair<Address, Address> getDomain() {
        final Address successorDistance;
        synchronized (this) {
            successorDistance = this.successors.getNeighbor();
        }
        if (successorDistance == null) {
            return new Pair<>(this.base, this.base.subtractOne());
        } else {
            return new Pair<>(this.base, this.peerCache.get(successorDistance).getAddress().subtractOne());
        }
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.successors.isEmpty();
    }

    @Override
    public boolean dominates(final Address target) {
        final Address distance;
        synchronized (this) {
            distance = this.successors.getNeighbor();
        }
        if (distance == null) {
            return true;
        } else {
            return this.base.distanceTo(target).compareTo(distance) < 0;
        }
    }

    @Override
    public synchronized AddressedPeer getRoutingDestination(final Address target) {
        final Address distance = this.base.distanceTo(target);
        final Address destinationDistance = this.fingers.getRoutingDestination(distance);
        if (destinationDistance != null) {
            // 普通に転送先候補の中から選べた。
            return this.peerCache.get(destinationDistance);
        } else {
            // 転送できるとしたら隣の個体。
            final Address successorDistance = this.successors.getNeighbor();
            if (successorDistance != null && successorDistance.compareTo(distance) <= 0) {
                return this.peerCache.get(successorDistance);
            } else {
                return null;
            }
        }
    }

    @Override
    public List<AddressedPeer> getSuccessors(final int maxHop) {
        final List<Address> distances;
        synchronized (this) {
            distances = this.successors.getNeighbors(maxHop);
        }
        final List<AddressedPeer> peers = new ArrayList<>(distances.size());
        for (final Address distance : distances) {
            peers.add(this.peerCache.get(distance));
        }
        return peers;
    }

    @Override
    public synchronized List<AddressedPeer> getPredecessors(final int maxHop) {
        final List<Address> distances = this.predecessors.getNeighbors(maxHop);
        final List<AddressedPeer> peers = new ArrayList<>(distances.size());
        for (final Address distance : distances) {
            peers.add(this.peerCache.get(distance));
        }
        return peers;
    }

    @Override
    public synchronized List<AddressedPeer> getFingers() {
        final List<Address> distances = this.fingers.getAll();
        final List<AddressedPeer> peers = new ArrayList<>(distances.size());
        for (final Address distance : distances) {
            peers.add(this.peerCache.get(distance));
        }
        return peers;
    }

    @Override
    public synchronized List<AddressedPeer> getImportantPeers() {
        final List<Address> successorDistances = this.successors.getAll();
        final List<Address> predecessorDistances = this.predecessors.getAll();
        final List<Address> fingerDistances = this.fingers.getAll();
        final Set<Address> distances = new HashSet<>(successorDistances);
        distances.addAll(predecessorDistances);
        distances.addAll(fingerDistances);
        final List<AddressedPeer> peers = new ArrayList<>(distances.size());
        for (final Address distance : distances) {
            peers.add(this.peerCache.get(distance));
        }
        return peers;
    }

    @Override
    public synchronized List<AddressedPeer> getPeers() {
        return this.peerCache.getAll();
    }

    @Override
    public synchronized Address estimateAverageDistance() {
        return this.successors.getAverageDistance();
    }

    /**
     * 既に把握している個体が構造の一部であるかどうか。
     * @param peerDistance 把握している個体への距離
     * @return 構造の一部である場合のみ true
     */
    private boolean IsImportantContainedPeer(final Address peerDistance) {
        return !this.fingers.isEmpty()
                && (peerDistance.equals(this.fingers.getRoutingDestination(peerDistance))
                        || peerDistance.compareTo(this.successors.getFarthestNeighbor()) <= 0
                        || this.predecessors.getFarthestNeighbor().compareTo(peerDistance) <= 0);
    }

    private void trimCache() {
        /*
         * 無限ループ防止装置。
         * お世話にならないためにも capacity は大きめにしよう (最低 100、推奨 1000 以上)。
         */
        int numOfFailures = 0;
        while (this.peerCache.size() > this.capacity && numOfFailures < this.peerCache.size()) {
            final Map.Entry<Address, AddressedPeer> eldest = this.peerCache.removeEldest();
            if (IsImportantContainedPeer(eldest.getKey())) {
                // 使用されている場合は差し戻す。
                this.peerCache.put(eldest.getKey(), eldest.getValue());
                numOfFailures++;
            }
        }
    }

    @Override
    public synchronized boolean addPeer(final AddressedPeer peer) {
        if (peer.getAddress().equals(this.base)) {
            return false;
        }

        final Address peerDistance = this.base.distanceTo(peer.getAddress());
        final AddressedPeer oldPeer = this.peerCache.get(peerDistance);
        if (oldPeer != null) {
            // 論理位置は登録済み。
            if (oldPeer.getPeer().equals(peer.getPeer())) {
                // 既に登録済み。
                return false;
            } else {
                // IPアドレスが変わった。
                this.peerCache.put(peerDistance, peer);
                return IsImportantContainedPeer(peerDistance);
            }
        }

        // 論理位置は登録されていない。

        boolean modified = false;
        final Address oldDistance = this.peerCache.getDistance(peer.getPeer());
        if (oldDistance != null) {
            // IPアドレスは登録してあるが、論理位置が違う。
            this.peerCache.remove(peer.getPeer());
            modified |= removePeerCore(oldDistance);
        }

        modified |= this.fingers.add(peerDistance);
        modified |= this.successors.add(peerDistance);
        modified |= this.predecessors.add(peerDistance);

        this.peerCache.put(peerDistance, peer);
        trimCache();

        return modified;
    }

    private boolean removePeerCore(final Address peerDistance) {
        boolean modified = false;
        if (this.fingers.remove(peerDistance)) {
            modified = true;
            // 近道の修復。
            final int distanceLevel = CcFunctions.distanceLevel(peerDistance);
            final Map.Entry<Address, AddressedPeer> entry;
            if (distanceLevel < Address.SIZE) {
                final Address point = Address.ZERO.addPowerOfTwo(distanceLevel);
                entry = this.peerCache.searchSmallerOrEquals(point);
            } else {
                entry = this.peerCache.getLargest();
            }
            if (entry != null) {
                this.fingers.add(entry.getKey());
            }
        }
        if (this.successors.remove(peerDistance)) {
            modified = true;
            // ご近所の修復。
            Address previous = (this.successors.isEmpty() ? Address.ZERO : this.successors.getFarthestNeighbor());
            while (true) {
                final Map.Entry<Address, AddressedPeer> entry = this.peerCache.searchLarger(previous);
                if (entry == null || !this.successors.add(entry.getKey())) {
                    break;
                }
                previous = entry.getKey();
            }
        }
        if (this.predecessors.remove(peerDistance)) {
            modified = true;
            // ご近所の修復。
            Address previous = (this.predecessors.isEmpty() ? Address.ZERO : this.predecessors.getFarthestNeighbor());
            while (true) {
                final Map.Entry<Address, AddressedPeer> entry = this.peerCache.searchSmaller(previous);
                if (entry == null || !this.predecessors.add(entry.getKey())) {
                    break;
                }
                previous = entry.getKey();
            }
        }
        return modified;
    }

    @Override
    public synchronized Address removePeer(final InetSocketAddress peer) {
        final Map.Entry<Address, AddressedPeer> peerEntry = this.peerCache.remove(peer);
        if (peerEntry == null) {
            // 元々無い。
            return null;
        }

        if (removePeerCore(peerEntry.getKey())) {
            return peerEntry.getValue().getAddress();
        } else {
            return null;
        }
    }

    @Override
    public synchronized String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.base)
                .append(", ").append(this.successors)
                .append(", ").append(this.predecessors)
                .append(", ").append(this.fingers)
                .append(']').toString();
    }

    public static void main(final String[] args) throws UnknownHostException {
        final CcView instance = new BasicCcView(new Address(BigInteger.valueOf(0xed9e305cL), 32), 1_000);
        for (final AddressedPeer peer : new AddressedPeer[] {
                // new AddressedPeer(new Address(BigInteger.valueOf(0xd96c6fa0L), 32), new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                // (byte) 168, 95, 5 }), 24809)),
                // new AddressedPeer(new Address(BigInteger.valueOf(0x78a16722L), 32), new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                // (byte) 168, 95, 5 }), 24843)),
                // new AddressedPeer(new Address(BigInteger.valueOf(0x7a93d57fL), 32), new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                // (byte) 168, 95, 5 }), 24820)),
                // new AddressedPeer(new Address(BigInteger.valueOf(0x501c0c1fL), 32), new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                // (byte) 168, 95, 5 }), 24847)),
                // new AddressedPeer(new Address(BigInteger.valueOf(0x96d34e5dL), 32), new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                // (byte) 168, 95, 5 }), 24846)),
                // new AddressedPeer(new Address(BigInteger.valueOf(0x5ba9a4eeL), 32), new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                // (byte) 168, 95, 5 }), 24843)),
                // new AddressedPeer(new Address(BigInteger.valueOf(0xd9544c52L), 32), new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                // (byte) 168, 95, 5 }), 24833)),
                // new AddressedPeer(new Address(BigInteger.valueOf(0x584e2e8bL), 32), new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                // (byte) 168, 95, 5 }), 24827)),
                // new AddressedPeer(new Address(BigInteger.valueOf(0x652c2f57L), 32), new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                // (byte) 168, 95, 5 }), 24816)),
                // new AddressedPeer(new Address(BigInteger.valueOf(0xd96c6fa0L), 32), new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                // (byte) 168, 95, 5 }), 24809)),
                // new AddressedPeer(new Address(BigInteger.valueOf(0x652c2f57L), 32), new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                // (byte) 168, 95, 5 }), 24816)),
                new AddressedPeer(new Address(BigInteger.valueOf(0x5d4f432cL), 32), new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                        (byte) 168, 95, 5 }), 24826)),
                new AddressedPeer(new Address(BigInteger.valueOf(0x3333432cL), 32), new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                        (byte) 168, 95, 5 }), 24826)),
                new AddressedPeer(new Address(BigInteger.valueOf(0x444f432cL), 32), new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                        (byte) 168, 95, 5 }), 24826)),
        }) {
            instance.addPeer(peer);
        }
        System.out.println(instance);
        System.out.println(instance.getImportantPeers());
        instance.removePeer(new InetSocketAddress(InetAddress.getByAddress(new byte[] { (byte) 192,
                (byte) 168, 95, 5 }), 24843));
        System.out.println(instance.getImportantPeers());
    }
}
