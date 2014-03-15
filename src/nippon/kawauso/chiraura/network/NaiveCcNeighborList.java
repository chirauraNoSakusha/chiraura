package nippon.kawauso.chiraura.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;

/**
 * CustomChord での論理空間正方向のご近所さん。
 * @author chirauraNoSakusha
 */
final class NaiveCcNeighborList implements CcNeighborList {

    private final NavigableSet<Address> neighborDistances;

    NaiveCcNeighborList() {
        this.neighborDistances = new TreeSet<>();
    }

    @Override
    public boolean isEmpty() {
        return this.neighborDistances.isEmpty();
    }

    @Override
    public int getCurrentCapacity() {
        if (this.neighborDistances.isEmpty()) {
            return 1;
        } else {
            return CcFunctions.estimateNumOfNeighbors(this.neighborDistances.last().toBigInteger(), this.neighborDistances.size());
        }
    }

    @Override
    public Address getNeighbor() {
        if (this.neighborDistances.isEmpty()) {
            return null;
        } else {
            return this.neighborDistances.first();
        }
    }

    @Override
    public Address getFarthestNeighbor() {
        if (this.neighborDistances.isEmpty()) {
            return null;
        } else {
            return this.neighborDistances.last();
        }
    }

    @Override
    public List<Address> getNeighbors(final int maxHop) {
        if (maxHop >= this.neighborDistances.size()) {
            return new ArrayList<>(this.neighborDistances);
        } else {
            final List<Address> neighbors = new ArrayList<>(maxHop);
            for (final Address distance : this.neighborDistances) {
                if (neighbors.size() >= maxHop) {
                    break;
                }
                neighbors.add(distance);
            }
            return neighbors;
        }
    }

    @Override
    public List<Address> getAll() {
        return new ArrayList<>(this.neighborDistances);
    }

    @Override
    public Address getAverageDistance() {
        if (this.neighborDistances.isEmpty()) {
            return null;
        } else {
            return this.neighborDistances.last().divide(this.neighborDistances.size());
        }
    }

    @Override
    public boolean add(final Address peerDistance) {
        if (this.neighborDistances.isEmpty()) {
            this.neighborDistances.add(peerDistance);
            return true;
        }

        final Address longestDistance = this.neighborDistances.last();

        final int flag = peerDistance.compareTo(longestDistance);
        if (flag == 0) {
            return false;
        } else if (flag < 0) {
            // 途中に挿入。
            if (!this.neighborDistances.add(peerDistance)) {
                // 既に存在。
                return false;
            }

            while (this.neighborDistances.size() > getCurrentCapacity()) {
                this.neighborDistances.pollLast();
            }
            return true;
        } else {
            // 挿入するなら末尾。
            final int afterCapacity = CcFunctions.estimateNumOfNeighbors(peerDistance.toBigInteger(), this.neighborDistances.size() + 1);
            if (this.neighborDistances.size() < afterCapacity) {
                this.neighborDistances.add(peerDistance);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean remove(final Address peerDistance) {
        if (!this.neighborDistances.remove(peerDistance)) {
            // 元々無い。
            return false;
        } else {
            while (this.neighborDistances.size() > getCurrentCapacity()) {
                this.neighborDistances.pollLast();
            }
            return true;
        }
    }

    @Override
    public void clear() {
        this.neighborDistances.clear();
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.neighborDistances.size())
                .append(", {");
        boolean first = true;
        for (final Address distance : this.neighborDistances) {
            if (first) {
                first = false;
            } else {
                buff.append(", ");
            }
            buff.append(distance);
        }
        return buff.append("}]").toString();
    }

    public static void main(final String[] args) {
        final NaiveCcNeighborList instance = new NaiveCcNeighborList();
        final List<Address> list = new ArrayList<>();
        final Address base = new Address(HashValue.calculateFromString("そうです。私が基点です。").toBigInteger(), HashValue.SIZE);
        for (int i = 0; i < 10; i++) {
            final Address address = new Address(HashValue.calculateFromString("そうです。私が基点です。" + i).toBigInteger(), HashValue.SIZE);
            instance.add(base.distanceTo(address));
            list.add(base.distanceTo(address));
        }
        System.out.println(instance);
        Collections.sort(list);
        for (final Address distance : list) {
            System.out.print(CcFunctions.distanceLevel(distance) + ":" + distance + ", ");
        }
    }

}
