package nippon.kawauso.chiraura.network;

import nippon.kawauso.chiraura.lib.base.Address;

/**
 * 論理位置への接触要請。
 * @author chirauraNoSakusha
 */
public final class AddressAccessRequest implements NetworkTask {

    private final Address address;

    AddressAccessRequest(final Address address) {
        this.address = address;
    }

    /**
     * 目標の論理位置を返す。
     * @return 目標の論理位置
     */
    public Address getAddress() {
        return this.address;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.address)
                .append(']').toString();
    }

}
