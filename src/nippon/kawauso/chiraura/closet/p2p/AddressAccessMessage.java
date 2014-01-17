/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.Message;

/**
 * 論理位置の支配者を確認するための言付け。
 * @author chirauraNoSakusha
 */
final class AddressAccessMessage implements Message {

    private final Address address;

    AddressAccessMessage(final Address address) {
        if (address == null) {
            throw new IllegalArgumentException("Null address.");
        }

        this.address = address;
    }

    Address getAddress() {
        return this.address;
    }

    @Override
    public int byteSize() {
        return this.address.byteSize();
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return this.address.toStream(output);
    }

    static BytesConvertible.Parser<AddressAccessMessage> getParser() {
        return new BytesConvertible.Parser<AddressAccessMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super AddressAccessMessage> output) throws MyRuleException,
                    IOException {
                final List<Address> target = new ArrayList<>(1);
                final int size = Address.getParser().fromStream(input, maxByteSize, target);
                output.add(new AddressAccessMessage(target.get(0)));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.address)
                .append(']').toString();
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof AddressAccessMessage)) {
            return false;
        }
        final AddressAccessMessage other = (AddressAccessMessage) obj;
        return this.address.equals(other.address);
    }

}
