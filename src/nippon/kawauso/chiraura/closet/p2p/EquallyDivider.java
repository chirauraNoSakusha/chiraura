package nippon.kawauso.chiraura.closet.p2p;

import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nippon.kawauso.chiraura.lib.base.Address;

/**
 * テスト用の計算機。
 * 先着順で2のべき乗の個体が等間隔に並ぶ。
 * @author chirauraNoSakusha
 */
public final class EquallyDivider implements AddressCalculator {

    private final Map<PublicKey, Address> cache;

    /**
     * 作成する。
     */
    public EquallyDivider() {
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public Address calculate(final PublicKey id) {
        Address address = this.cache.get(id);
        if (address == null) {
            synchronized (this) {
                address = this.cache.get(id);
                if (address == null) {
                    // ダブルチェックだが、ConcurrentHashMap は同期されているので問題無し。
                    address = Address.ZERO.addReverseBits(this.cache.size());
                    this.cache.put(id, address);
                }
            }
        }
        return address;
    }
}
