/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;
import java.util.Map;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.container.BasicInputOrderedMap;
import nippon.kawauso.chiraura.lib.container.InputOrderedMap;

/**
 * @author chirauraNoSakusha
 */
final class AddressLog {

    private static final class Entry {
        private final Address address;
        private final long date;

        private Entry(final Address address, final long date) {
            if (address == null) {
                throw new IllegalArgumentException("Null address.");
            }
            this.address = address;
            this.date = date;
        }

    }

    private final int limit;
    private final long duration;
    private final InputOrderedMap<InetSocketAddress, Entry> entries;

    AddressLog(final int limit, final long duration) {
        if (limit < 0) {
            throw new IllegalArgumentException("Negative limit ( " + limit + " ).");
        } else if (duration < 0) {
            throw new IllegalArgumentException("Negative duration ( " + duration + " ).");
        }
        this.limit = limit;
        this.duration = duration;

        this.entries = new BasicInputOrderedMap<>();
    }

    private void trim(final long cur) {
        while (this.limit < this.entries.size()) {
            this.entries.removeEldest();
        }
        while (!this.entries.isEmpty()) {
            final Map.Entry<InetSocketAddress, Entry> eldest = this.entries.getEldest();
            if (eldest.getValue().date + this.duration < cur) {
                this.entries.remove(eldest.getKey());
            } else {
                break;
            }
        }
    }

    /**
     * 個体を含んでいて、かつ、その論理位置が指定と異なるかどうかを検査。
     * @param peer 調べる個体
     * @param address 調べる論理位置
     * @return 個体を含んでいて、かつ、その論理位置が指定と異なるなら true
     */
    synchronized boolean containsAndNotEquals(final InetSocketAddress peer, final Address address) {
        trim(System.currentTimeMillis());
        final Entry entry = this.entries.get(peer);
        return entry != null && !entry.address.equals(address);
    }

    /**
     * 個体とその論理位置を登録する。
     * @param peer 登録する個体
     * @param address 登録する論理位置
     */
    synchronized void add(final InetSocketAddress peer, final Address address) {
        final long cur = System.currentTimeMillis();
        trim(cur);
        this.entries.put(peer, new Entry(address, cur));
    }

    /**
     * 個体とその論理位置を削除する。
     * @param peer 削除する個体
     */
    synchronized void remove(final InetSocketAddress peer) {
        trim(System.currentTimeMillis());
        this.entries.remove(peer);
    }

}
