package nippon.kawauso.chiraura.network;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.container.Pair;

/**
 * 1つの個体から見える CostomChord の構造。
 * @author chirauraNoSakusha
 */
final class NaiveCcView implements CcView {

    private final Address base;

    private final NavigableMap<Address, AddressedPeer> distanceToPeer;
    private final Map<InetSocketAddress, Address> peerToDistance;

    NaiveCcView(final Address base) {
        if (base == null) {
            throw new IllegalArgumentException("Null base.");
        }
        this.base = base;
        this.distanceToPeer = new TreeMap<>();
        this.peerToDistance = new HashMap<>();
    }

    @Override
    public Address getBase() {
        return this.base;
    }

    @Override
    public Pair<Address, Address> getDomain() {
        Address next;
        if (this.distanceToPeer.isEmpty()) {
            next = this.base;
        } else {
            next = this.distanceToPeer.firstKey();
        }
        return new Pair<>(this.base, next.subtractOne());
    }

    @Override
    public boolean isEmpty() {
        return this.distanceToPeer.isEmpty();
    }

    @Override
    public boolean dominates(final Address address) {
        if (this.distanceToPeer.isEmpty()) {
            return true;
        } else {
            return this.base.distanceTo(address).compareTo(this.distanceToPeer.firstKey()) < 0;
        }
    }

    private Map.Entry<Address, AddressedPeer> getRoutingFingerEntry(final Address targetDistance) {
        final int distanceLevel = CcFunctions.distanceLevel(targetDistance);
        Map.Entry<Address, AddressedPeer> destinationEntry;
        if (distanceLevel == Address.SIZE) {
            destinationEntry = this.distanceToPeer.lastEntry();
        } else {
            destinationEntry = this.distanceToPeer.floorEntry(Address.ZERO.addPowerOfTwo(distanceLevel));
            if (destinationEntry == null) {
                return null;
            }
        }

        if (targetDistance.compareTo(destinationEntry.getKey()) < 0) {
            if (distanceLevel == 0) {
                return null;
            } else {
                destinationEntry = this.distanceToPeer.floorEntry(Address.ZERO.addPowerOfTwo(distanceLevel - 1));
                if (destinationEntry == null) {
                    return null;
                }
            }
        }

        return destinationEntry;
    }

    @Override
    public AddressedPeer getRoutingDestination(final Address target) {
        final Map.Entry<Address, AddressedPeer> entry = getRoutingDestinationEntry(target);
        if (entry == null) {
            return null;
        } else {
            return entry.getValue();
        }
    }

    private Map.Entry<Address, AddressedPeer> getRoutingDestinationEntry(final Address target) {
        if (this.distanceToPeer.isEmpty()) {
            return null;
        }

        final Address targetDistance = this.base.distanceTo(target);
        final Map.Entry<Address, AddressedPeer> fingerEntry = getRoutingFingerEntry(targetDistance);
        if (fingerEntry != null) {
            return fingerEntry;
        } else {
            final Map.Entry<Address, AddressedPeer> successorEntry = this.distanceToPeer.firstEntry();
            if (successorEntry.getKey().compareTo(targetDistance) <= 0) {
                return successorEntry;
            } else {
                return null;
            }
        }
    }

    @Override
    public List<AddressedPeer> getSuccessors(final int maxHop) {
        final List<AddressedPeer> neighbors = new ArrayList<>();
        for (final Map.Entry<Address, AddressedPeer> entry : getSuccessorEntries()) {
            if (neighbors.size() >= maxHop) {
                break;
            }
            neighbors.add(entry.getValue());
        }
        return neighbors;
    }

    private List<Map.Entry<Address, AddressedPeer>> getSuccessorEntries() {
        final List<Map.Entry<Address, AddressedPeer>> entries = new ArrayList<>();
        if (this.distanceToPeer.isEmpty()) {
            return entries;
        }

        for (final Map.Entry<Address, AddressedPeer> entry : this.distanceToPeer.entrySet()) {
            final int afterCapacity = CcFunctions.estimateNumOfNeighbors(entry.getKey().toBigInteger(), entries.size() + 1);
            if (entries.size() + 1 > afterCapacity) {
                break;
            }
            entries.add(entry);
        }
        return entries;

    }

    @Override
    public List<AddressedPeer> getPredecessors(final int maxHop) {
        final List<AddressedPeer> neighbors = new ArrayList<>();
        for (final Map.Entry<Address, AddressedPeer> entry : getPredecessorEntries()) {
            if (neighbors.size() >= maxHop) {
                break;
            }
            neighbors.add(entry.getValue());
        }
        return neighbors;
    }

    private List<Map.Entry<Address, AddressedPeer>> getPredecessorEntries() {
        final List<Map.Entry<Address, AddressedPeer>> entries = new ArrayList<>();
        if (this.distanceToPeer.isEmpty()) {
            return entries;
        }

        for (final Map.Entry<Address, AddressedPeer> entry : this.distanceToPeer.descendingMap().entrySet()) {
            final int afterCapacity = CcFunctions.estimateNumOfNeighbors(entry.getKey().distanceTo(Address.ZERO).toBigInteger(), entries.size() + 1);
            if (entries.size() + 1 > afterCapacity) {
                break;
            }
            entries.add(entry);
        }
        return entries;

    }

    @Override
    public List<AddressedPeer> getFingers() {
        final List<AddressedPeer> shortcuts = new ArrayList<>();
        for (final Map.Entry<Address, AddressedPeer> entry : getFingerEntries()) {
            shortcuts.add(entry.getValue());
        }
        return shortcuts;
    }

    private List<Map.Entry<Address, AddressedPeer>> getFingerEntries() {
        final List<Map.Entry<Address, AddressedPeer>> entries = new ArrayList<>();
        if (this.distanceToPeer.isEmpty()) {
            return entries;
        }

        final Map.Entry<Address, AddressedPeer> first = this.distanceToPeer.lastEntry();
        entries.add(first);
        for (int i = CcFunctions.distanceLevel(first.getKey()) - 1; i >= 0; i--) {
            final Map.Entry<Address, AddressedPeer> entry = this.distanceToPeer.floorEntry(Address.ZERO.addPowerOfTwo(i));
            if (entry == null) {
                break;
            }
            entries.add(entry);
            i = CcFunctions.distanceLevel(entry.getKey());
        }
        Collections.reverse(entries);

        return entries;
    }

    @Override
    public List<AddressedPeer> getImportantPeers() {
        final Map<Address, AddressedPeer> buff = new HashMap<>();
        for (final Map.Entry<Address, AddressedPeer> entry : getSuccessorEntries()) {
            buff.put(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<Address, AddressedPeer> entry : getPredecessorEntries()) {
            buff.put(entry.getKey(), entry.getValue());
        }
        for (final Map.Entry<Address, AddressedPeer> entry : getFingerEntries()) {
            buff.put(entry.getKey(), entry.getValue());
        }
        return new ArrayList<>(buff.values());
    }

    @Override
    public List<AddressedPeer> getPeers() {
        return new ArrayList<>(this.distanceToPeer.values());
    }

    @Override
    public Address estimateAverageDistance() {
        if (this.distanceToPeer.isEmpty()) {
            return null;
        } else {
            final List<Map.Entry<Address, AddressedPeer>> successors = getSuccessorEntries();
            return successors.get(successors.size() - 1).getKey().divide(successors.size());
        }
    }

    /**
     * 既に把握している個体が構造の一部であるかどうか。
     * @param peerDistance 把握している個体への距離
     * @return 構造の一部である場合のみ true
     */
    private boolean IsImportantContainedPeer(final Address peerDistance) {
        if (this.distanceToPeer.isEmpty()) {
            return false;
        }
        final Map.Entry<Address, AddressedPeer> fingerEntry = getRoutingFingerEntry(peerDistance);
        final List<Map.Entry<Address, AddressedPeer>> successorEntries = getSuccessorEntries();
        final List<Map.Entry<Address, AddressedPeer>> predecessorEntries = getPredecessorEntries();
        return (fingerEntry != null && fingerEntry.getKey().equals(peerDistance))
                || peerDistance.compareTo(successorEntries.get(successorEntries.size() - 1).getKey()) <= 0
                || predecessorEntries.get(predecessorEntries.size() - 1).getKey().compareTo(peerDistance) <= 0;
    }

    @Override
    public boolean addPeer(final AddressedPeer peer) {
        if (this.base.equals(peer.getAddress())) {
            return false;
        }

        final Address peerDistance = this.base.distanceTo(peer.getAddress());
        final AddressedPeer oldPeer = this.distanceToPeer.get(peerDistance);
        if (oldPeer != null) {
            // 論理位置は登録済み。
            if (oldPeer.getPeer().equals(peer.getPeer())) {
                // 既に登録済み。
                return false;
            } else {
                // IPアドレスが変わった。
                this.distanceToPeer.put(peerDistance, peer);
                this.peerToDistance.put(peer.getPeer(), peerDistance);
                return IsImportantContainedPeer(peerDistance);
            }
        }

        boolean modified = false;
        final Address oldDistance = this.peerToDistance.get(peer.getPeer());
        if (oldDistance != null) {
            // IPアドレスは登録してあるけど、論理位置は登録されてない (変わってる)。
            modified |= removePeerCore(oldDistance);
        }

        this.distanceToPeer.put(peerDistance, peer);
        this.peerToDistance.put(peer.getPeer(), peerDistance);
        return modified || IsImportantContainedPeer(peerDistance);
    }

    private boolean removePeerCore(final Address peerDistance) {
        final boolean modified = IsImportantContainedPeer(peerDistance);
        return this.distanceToPeer.remove(peerDistance) != null && modified;
    }

    @Override
    public Address removePeer(final InetSocketAddress peer) {
        final Address peerDistance = this.peerToDistance.remove(peer);
        if (peerDistance == null) {
            // 元々無い。
            return null;
        }

        final Address peerAddress = this.distanceToPeer.get(peerDistance).getAddress();
        if (removePeerCore(peerDistance)) {
            return peerAddress;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        final StringBuilder buff = new StringBuilder(this.getClass().getSimpleName())
                .append('[').append(this.base).append(", ");
        toStringSub(buff, getSuccessorEntries());
        buff.append(", ");
        toStringSub(buff, getPredecessorEntries());
        buff.append(", ");
        toStringSub(buff, getFingerEntries());
        return buff.toString();
    }

    private static void toStringSub(final StringBuilder buff, final List<Map.Entry<Address, AddressedPeer>> list) {
        buff.append("[").append(list.size()).append(", {");
        boolean first = true;
        for (final Map.Entry<Address, ?> entry : list) {
            if (first) {
                first = false;
            } else {
                buff.append(", ");
            }
            buff.append(entry.getKey());
        }
        buff.append("}]");
    }
}
