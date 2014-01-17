/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;

/**
 * CustomChord での通信先候補となる個体を管理する。
 * getRoutingDestination() を定数負荷で実行できる。
 * 無駄にメモリを浪費しない。
 * @author chirauraNoSakusha
 */
final class BasicCcShortcutTable implements CcShortcutTable {

    /*
     * shortcutDistances[i] に距離レベルが Address.SIZE - i 以下で最も遠い個体を入れる。
     * 逆に言うと、距離レベルが k の個体は shortcutDistances[Address.SIZE - k] 以前にしか入らない。
     */
    private final ArrayList<Address> shortcutDistances;

    BasicCcShortcutTable() {
        this.shortcutDistances = new ArrayList<>();
    }

    @Override
    public boolean isEmpty() {
        return this.shortcutDistances.isEmpty();
    }

    @Override
    public Address getRoutingDestination(final Address targetDistance) {
        final int index = Address.SIZE - CcFunctions.distanceLevel(targetDistance);
        if (index >= this.shortcutDistances.size()) {
            return null;
        } else if (this.shortcutDistances.get(index).compareTo(targetDistance) <= 0) {
            return this.shortcutDistances.get(index);
        } else if (index + 1 >= this.shortcutDistances.size()) {
            return null;
        } else {
            return this.shortcutDistances.get(index + 1);
        }
    }

    @Override
    public List<Address> getAll() {
        final List<Address> uniques = new ArrayList<>();
        Address previous = null;
        for (final Address distance : this.shortcutDistances) {
            if (distance != previous) {
                uniques.add(distance);
                previous = distance;
            }
        }
        Collections.reverse(uniques);
        return uniques;
    }

    @Override
    public boolean add(final Address peerDistance) {
        final int index = Address.SIZE - CcFunctions.distanceLevel(peerDistance);
        if (index >= this.shortcutDistances.size()) {
            for (int i = this.shortcutDistances.size(); i <= index; i++) {
                this.shortcutDistances.add(peerDistance);
            }
            return true;
        } else if (this.shortcutDistances.get(index).compareTo(peerDistance) < 0) {
            this.shortcutDistances.set(index, peerDistance);
            for (int i = index - 1; i >= 0 && this.shortcutDistances.get(i).compareTo(peerDistance) < 0; i--) {
                this.shortcutDistances.set(i, peerDistance);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean remove(final Address peerDistance) {
        final int index = Address.SIZE - CcFunctions.distanceLevel(peerDistance);
        boolean modified = false;
        if (index == this.shortcutDistances.size() - 1) {
            for (int i = index; i >= 0; i--) {
                if (!peerDistance.equals(this.shortcutDistances.get(i))) {
                    break;
                }
                this.shortcutDistances.remove(i);
                modified = true;
            }
        } else if (index < this.shortcutDistances.size() - 1) {
            for (int i = index; i >= 0; i--) {
                if (!peerDistance.equals(this.shortcutDistances.get(i))) {
                    break;
                }
                this.shortcutDistances.set(i, this.shortcutDistances.get(i + 1));
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        this.shortcutDistances.clear();
    }

    @Override
    public String toString() {
        final List<Address> uniques = new ArrayList<>();
        Address previous = null;
        for (final Address distance : this.shortcutDistances) {
            if (distance != previous) {
                uniques.add(distance);
                previous = distance;
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
            buff.append(Integer.toString(CcFunctions.distanceLevel(distance))).append(':').append(distance);
        }
        return buff.append("}]").toString();
    }

    public static void main(final String[] args) {
        final BasicCcShortcutTable instance = new BasicCcShortcutTable();
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
