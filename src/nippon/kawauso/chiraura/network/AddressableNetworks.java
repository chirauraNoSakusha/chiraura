package nippon.kawauso.chiraura.network;

import nippon.kawauso.chiraura.lib.base.Address;


/**
 * @author chirauraNoSakusha
 */
public final class AddressableNetworks {

    // インスタンス化防止。
    private AddressableNetworks() {}

    /**
     * 分散メモリっぽいものの管理構造を作成する。
     * @param self 自分の論理位置
     * @param peerCapacity 把握する個体の最大数
     * @param maintenanceInterval 維持活動の周期 (ミリ秒)
     * @return 分散メモリっぽいものの管理構造
     */
    public static AddressableNetwork newInstance(final Address self, final int peerCapacity, final long maintenanceInterval) {
        return new CustomChord(self, peerCapacity, maintenanceInterval);
    }

}
