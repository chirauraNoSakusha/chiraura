/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.security.PublicKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;

/**
 * ハッシュ値を用いる個体の論理位置計算機。
 * 並列対応。
 * @author chirauraNoSakusha
 */
public final class HashingCalculator implements AddressCalculator {

    private final Map<PublicKey, Address> cache;

    /**
     * 計算結果を記憶する数を指定して作成する。
     * @param cacheCapacity 計算結果を記憶する数
     */
    public HashingCalculator(final int cacheCapacity) {
        if (cacheCapacity < 0) {
            throw new IllegalArgumentException("Negative capacity ( " + cacheCapacity + " ).");
        }

        @SuppressWarnings("serial")
        final Map<PublicKey, Address> base = new LinkedHashMap<PublicKey, Address>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(final Map.Entry<PublicKey, Address> eldest) {
                return size() > cacheCapacity;
            }
        };
        this.cache = Collections.synchronizedMap(base);
    }

    @Override
    public Address calculate(final PublicKey id) {
        Address address = this.cache.get(id);
        if (address == null) {
            address = new Address(HashValue.calculateFromBytes(id.getEncoded()).toBigInteger(), HashValue.SIZE);
            this.cache.put(id, address);
        }
        return address;
    }

}
