package nippon.kawauso.chiraura.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;

/**
 * CustomChord での論理空間負方向のご近所さん。
 * getNeighbor() が定数負荷でできる。
 * 並列非対応。
 * @author chirauraNoSakusha
 */
final class CcPredecessorList implements CcNeighborList {

    /*
     * 引数に与えられる、基点から対象の論理位置までの距離から、
     * 対象の論理位置から基点までの距離を計算して用いる。
     */

    private final CcSuccessorList base;

    CcPredecessorList() {
        this.base = new CcSuccessorList();
    }

    @Override
    public boolean isEmpty() {
        return this.base.isEmpty();
    }

    @Override
    public int getCurrentCapacity() {
        return this.base.getCurrentCapacity();
    }

    @Override
    public Address getNeighbor() {
        final Address virtualDistance = this.base.getNeighbor();
        if (virtualDistance == null) {
            return null;
        } else {
            return virtualDistance.distanceTo(Address.ZERO);
        }
    }

    @Override
    public Address getFarthestNeighbor() {
        final Address virtualDistance = this.base.getFarthestNeighbor();
        if (virtualDistance == null) {
            return null;
        } else {
            return virtualDistance.distanceTo(Address.ZERO);
        }
    }

    @Override
    public List<Address> getNeighbors(final int maxHop) {
        final List<Address> virtualDistances = this.base.getNeighbors(maxHop);
        final List<Address> neighborDistances = new ArrayList<>(virtualDistances.size());
        for (final Address virtualDistance : virtualDistances) {
            neighborDistances.add(virtualDistance.distanceTo(Address.ZERO));
        }
        return neighborDistances;
    }

    @Override
    public List<Address> getAll() {
        final List<Address> virtualDistances = this.base.getAll();
        final List<Address> distances = new ArrayList<>(virtualDistances.size());
        for (final Address virtualDistance : virtualDistances) {
            distances.add(virtualDistance.distanceTo(Address.ZERO));
        }
        return distances;
    }

    @Override
    public Address getAverageDistance() {
        return this.base.getAverageDistance();
    }

    @Override
    public boolean add(final Address peerDistance) {
        return this.base.add(peerDistance.distanceTo(Address.ZERO));
    }

    @Override
    public boolean remove(final Address peerDistance) {
        return this.base.remove(peerDistance.distanceTo(Address.ZERO));
    }

    @Override
    public void clear() {
        this.base.clear();
    }

    @Override
    public String toString() {
        final List<Address> virtualDistances = this.base.getAll();
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(virtualDistances.size())
                .append(", {");
        boolean first = true;
        for (final Address virtualDistance : virtualDistances) {
            if (first) {
                first = false;
            } else {
                buff.append(", ");
            }
            buff.append(virtualDistance.distanceTo(Address.ZERO));
        }
        return buff.append("}]").toString();
    }

    public static void main(final String[] args) {
        final CcPredecessorList instance = new CcPredecessorList();
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
