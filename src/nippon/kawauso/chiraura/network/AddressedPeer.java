/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.BytesFunctions;
import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.connection.PeerCell;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 論理位置が特定されている個体。
 * @author chirauraNoSakusha
 */
public final class AddressedPeer implements Comparable<AddressedPeer>, BytesConvertible {

    private final Address address;
    private final InetSocketAddress peer;

    /**
     * 作成する。
     * @param address 論理位置
     * @param peer 個体
     */
    public AddressedPeer(final Address address, final InetSocketAddress peer) {
        if (address == null) {
            throw new IllegalArgumentException("Null address.");
        } else if (peer == null) {
            throw new IllegalArgumentException("Null peer.");
        }
        this.address = address;
        this.peer = peer;
    }

    /**
     * 論理位置を返す。
     * @return 論理位置
     */
    public Address getAddress() {
        return this.address;
    }

    /**
     * 個体を返す。
     * @return 個体
     */
    public InetSocketAddress getPeer() {
        return this.peer;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("oo", this.address, new PeerCell(this.peer));
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "oo", this.address, new PeerCell(this.peer));
    }

    /**
     * 復号器を得る。
     * @return 復号器
     */
    public static BytesConvertible.Parser<AddressedPeer> getParser() {
        return new BytesConvertible.Parser<AddressedPeer>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super AddressedPeer> output) throws MyRuleException, IOException {
                final List<Address> address = new ArrayList<>(1);
                final List<PeerCell> peer = new ArrayList<>(1);
                final int size = BytesConversion.fromStream(input, maxByteSize, "oo", address, Address.getParser(), peer, PeerCell.getParser());
                output.add(new AddressedPeer(address.get(0), peer.get(0).get()));
                return size;
            }
        };
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AddressedPeer)) {
            return false;
        }
        final AddressedPeer other = (AddressedPeer) obj;
        return this.peer.equals(other.peer);
    }

    @Override
    public int compareTo(final AddressedPeer other) {
        final int diff = this.address.compareTo(other.address);
        if (diff != 0) {
            // 論理位置が違う。
            return diff;
        } else if (this.peer.equals(other.peer)) {
            // 論理位置も個体識別子も一緒。
            return 0;
        } else {
            // 論理位置は一緒だが、個体識別子は違う。
            return BytesFunctions.compare(BytesConversion.toBytes(new PeerCell(this.peer)), BytesConversion.toBytes(new PeerCell(other.peer)));
        }
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.address)
                .append(", ").append(this.peer)
                .append(']').toString();
    }

}
