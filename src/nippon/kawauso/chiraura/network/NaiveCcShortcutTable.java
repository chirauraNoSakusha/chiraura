package nippon.kawauso.chiraura.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;

/**
 * CustomChord での通信先候補となる個体を管理する。
 * @author chirauraNoSakusha
 */
final class NaiveCcShortcutTable implements CcShortcutTable {

    NavigableMap<Integer, Address> levelToShortcut;

    NaiveCcShortcutTable() {
        this.levelToShortcut = new TreeMap<>();
    }

    @Override
    public boolean isEmpty() {
        return this.levelToShortcut.isEmpty();
    }

    @Override
    public Address getRoutingDestination(final Address targetDistance) {
        Map.Entry<Integer, Address> entry = this.levelToShortcut.floorEntry(CcFunctions.distanceLevel(targetDistance));
        if (entry == null) {
            return null;
        } else if (entry.getValue().compareTo(targetDistance) <= 0) {
            return entry.getValue();
        } else {
            entry = this.levelToShortcut.lowerEntry(entry.getKey());
            if (entry == null) {
                return null;
            } else {
                return entry.getValue();
            }
        }
    }

    @Override
    public List<Address> getAll() {
        return new ArrayList<>(this.levelToShortcut.values());
    }

    @Override
    public boolean add(final Address peerDistance) {
        final int distanceLevel = CcFunctions.distanceLevel(peerDistance);
        final Address old = this.levelToShortcut.get(distanceLevel);
        if (old == null) {
            this.levelToShortcut.put(distanceLevel, peerDistance);
            return true;
        } else if (old.compareTo(peerDistance) < 0) {
            this.levelToShortcut.put(distanceLevel, peerDistance);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean remove(final Address peerDistance) {
        final int distanceLevel = CcFunctions.distanceLevel(peerDistance);
        final Address old = this.levelToShortcut.get(distanceLevel);
        if (old != null && old.compareTo(peerDistance) == 0) {
            this.levelToShortcut.remove(distanceLevel);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        this.levelToShortcut.clear();
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.levelToShortcut.size())
                .append(", {");
        boolean first = true;
        for (final Map.Entry<Integer, Address> entry : this.levelToShortcut.entrySet()) {
            if (first) {
                first = false;
            } else {
                buff.append(", ");
            }
            buff.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return buff.append("}]").toString();
    }

    public static void main(final String[] args) {
        final NaiveCcShortcutTable instance = new NaiveCcShortcutTable();
        final Address base = new Address(HashValue.calculateFromString("そうです。私が基点です。").toBigInteger(), HashValue.SIZE);
        for (int i = 0; i < 10; i++) {
            final Address address = new Address(HashValue.calculateFromString("そうです。私が基点です。" + i).toBigInteger(), HashValue.SIZE);
            instance.add(base.distanceTo(address));
        }
        System.out.println(instance);
    }

}
