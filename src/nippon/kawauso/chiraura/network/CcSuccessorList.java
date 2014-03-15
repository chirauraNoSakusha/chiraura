package nippon.kawauso.chiraura.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;

/**
 * CustomChord での論理空間正方向のご近所さん。
 * getNeighbor() が定数負荷でできる。
 * 並列非対応。
 * @author chirauraNoSakusha
 */
final class CcSuccessorList implements CcNeighborList {

    private final ArrayList<Address> neighborDistances;

    CcSuccessorList() {
        this.neighborDistances = new ArrayList<>();
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
            return CcFunctions.estimateNumOfNeighbors(this.neighborDistances.get(this.neighborDistances.size() - 1).toBigInteger(),
                    this.neighborDistances.size());
        }
    }

    @Override
    public Address getNeighbor() {
        if (this.neighborDistances.isEmpty()) {
            return null;
        } else {
            return this.neighborDistances.get(0);
        }
    }

    @Override
    public Address getFarthestNeighbor() {
        if (this.neighborDistances.isEmpty()) {
            return null;
        } else {
            return this.neighborDistances.get(this.neighborDistances.size() - 1);
        }
    }

    @Override
    public List<Address> getNeighbors(final int maxHop) {
        if (this.neighborDistances.isEmpty()) {
            return new ArrayList<>(0);
        } else if (this.neighborDistances.size() <= maxHop) {
            return this.neighborDistances;
        } else {
            return this.neighborDistances.subList(0, maxHop);
        }
    }

    @Override
    public List<Address> getAll() {
        return this.neighborDistances;
    }

    @Override
    public Address getAverageDistance() {
        if (this.neighborDistances.isEmpty()) {
            return null;
        } else {
            return this.neighborDistances.get(this.neighborDistances.size() - 1).divide(this.neighborDistances.size());
        }
    }

    @Override
    public boolean add(final Address peerDistance) {
        final int point = Collections.binarySearch(this.neighborDistances, peerDistance);
        if (point >= 0) {
            // 既にある。
            return false;
        } else {
            final int insertPoint = -point - 1;
            if (insertPoint < this.neighborDistances.size()) {
                // 途中に挿入。
                this.neighborDistances.add(insertPoint, peerDistance);

                while (this.neighborDistances.size() > getCurrentCapacity()) {
                    this.neighborDistances.remove(this.neighborDistances.size() - 1);
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
    }

    @Override
    public boolean remove(final Address peerDistance) {
        final int point = Collections.binarySearch(this.neighborDistances, peerDistance);
        if (point < 0) {
            // 元々無い。
            return false;
        } else {
            this.neighborDistances.remove(point);
            while (this.neighborDistances.size() > getCurrentCapacity()) {
                this.neighborDistances.remove(this.neighborDistances.size() - 1);
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
        final CcSuccessorList instance = new CcSuccessorList();
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
