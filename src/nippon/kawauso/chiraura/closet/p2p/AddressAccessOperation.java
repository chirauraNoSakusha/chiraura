/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.base.Address;

/**
 * 論理位置の支配者確認。
 * @author chirauraNoSakusha
 */
final class AddressAccessOperation implements Operation {

    private final Address address;

    AddressAccessOperation(final Address address) {
        if (address == null) {
            throw new IllegalArgumentException("Null address.");
        }
        this.address = address;
    }

    /**
     * 目標論理位置を返す。
     * @return 目標論理位置
     */
    Address getAddress() {
        return this.address;
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof AddressAccessOperation)) {
            return false;
        }
        /*
         * 1 つの論理位置へは 1 つしか実行しない。
         */
        final AddressAccessOperation other = (AddressAccessOperation) obj;
        return this.address.equals(other.address);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.address)
                .append(']').toString();
    }

}
