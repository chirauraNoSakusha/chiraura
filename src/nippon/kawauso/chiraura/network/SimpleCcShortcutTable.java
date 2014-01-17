/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;

/**
 * CustomChord での通信先候補となる個体 (への距離) を管理する。
 * getRoutingDestination() を定数負荷で実行できる。
 * @author chirauraNoSakusha
 */
final class SimpleCcShortcutTable implements CcShortcutTable {

    /*
     * shortcutDistances[i] に距離レベルが i 以下で最も遠い個体への距離を入れる。
     */
    private final Address[] shortcutDistances;

    SimpleCcShortcutTable() {
        this.shortcutDistances = new Address[Address.SIZE + 1];
    }

    @Override
    public boolean isEmpty() {
        return this.shortcutDistances[this.shortcutDistances.length - 1] == null;
    }

    @Override
    public Address getRoutingDestination(final Address targetDistance) {
        final int index = CcFunctions.distanceLevel(targetDistance);
        if (this.shortcutDistances[index] == null) {
            return null;
        } else if (this.shortcutDistances[index].compareTo(targetDistance) <= 0) {
            return this.shortcutDistances[index];
        } else if (index <= 0 || this.shortcutDistances[index - 1] == null) {
            return null;
        } else {
            return this.shortcutDistances[index - 1];
        }
    }

    @Override
    public List<Address> getAll() {
        final List<Address> uniques = new ArrayList<>();
        Address previous = null;
        for (int i = this.shortcutDistances.length - 1; i >= 0; i--) {
            if (this.shortcutDistances[i] == null) {
                break;
            }
            if (this.shortcutDistances[i] != previous) {
                uniques.add(this.shortcutDistances[i]);
                previous = this.shortcutDistances[i];
            }
        }
        Collections.reverse(uniques);
        return uniques;
    }

    @Override
    public boolean add(final Address peerDistance) {
        final int index = CcFunctions.distanceLevel(peerDistance);
        if (this.shortcutDistances[index] == null) {
            for (int i = index; i < this.shortcutDistances.length && this.shortcutDistances[i] == null; i++) {
                this.shortcutDistances[i] = peerDistance;
            }
            return true;
        } else if (this.shortcutDistances[index].compareTo(peerDistance) < 0) {
            this.shortcutDistances[index] = peerDistance;
            for (int i = index + 1; i < this.shortcutDistances.length && this.shortcutDistances[i].compareTo(peerDistance) < 0; i++) {
                this.shortcutDistances[i] = peerDistance;
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean remove(final Address peerDistance) {
        final int index = CcFunctions.distanceLevel(peerDistance);
        boolean modified = false;
        for (int i = index; i < this.shortcutDistances.length; i++) {
            if (this.shortcutDistances[i] == null || !peerDistance.equals(this.shortcutDistances[i])) {
                break;
            }
            if (i > 0) {
                this.shortcutDistances[i] = this.shortcutDistances[i - 1];
            } else {
                this.shortcutDistances[i] = null;
            }
            modified = true;
        }
        return modified;
    }

    @Override
    public void clear() {
        Arrays.fill(this.shortcutDistances, null);
    }

    @Override
    public String toString() {
        final List<Address> uniques = new ArrayList<>();
        Address previous = null;
        for (int i = this.shortcutDistances.length - 1; i >= 0; i--) {
            if (this.shortcutDistances[i] == null) {
                break;
            }
            if (this.shortcutDistances[i] != previous) {
                uniques.add(this.shortcutDistances[i]);
                previous = this.shortcutDistances[i];
            }
        }
        Collections.reverse(uniques);
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(uniques.size())
                .append(", {");
        boolean first = true;
        for (final Address distance : uniques) {
            if (first) {
                first = false;
            } else {
                buff.append(", ");
            }
            buff.append(CcFunctions.distanceLevel(distance)).append(':').append(distance);
        }

        // // 以下、デバッグ用。
        // int start;
        // for (start = 0; start < this.table.length && this.table[start] == null; start++) {
        // }
        // buff.append(" {");
        // first = true;
        // for (int i = start; i < this.table.length; i++) {
        // if (first) {
        // first = false;
        // } else {
        // buff.append(", ");
        // }
        // buff.append(i).append(':').append(this.table[i]);
        // }
        // buff.append('}');

        return buff.append("}]").toString();
    }

    public static void main(final String[] args) {
        final SimpleCcShortcutTable instance = new SimpleCcShortcutTable();
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
