/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * 個体確認への返答。
 * 把握している個体一覧。
 * @author chirauraNoSakusha
 */
final class PeerAccessReply implements Message {

    private final List<AddressedPeer> peers;

    PeerAccessReply(final List<AddressedPeer> peers) {
        if (peers == null) {
            throw new IllegalArgumentException("Null peers.");
        }
        this.peers = peers;
    }

    List<AddressedPeer> getPeers() {
        return this.peers;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("ao", this.peers);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "ao", this.peers);
    }

    static BytesConvertible.Parser<PeerAccessReply> getParser() {
        return new BytesConvertible.Parser<PeerAccessReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super PeerAccessReply> output) throws MyRuleException,
                    IOException {
                final List<AddressedPeer> peers = new ArrayList<>();
                final int size = BytesConversion.fromStream(input, maxByteSize, "ao", peers, AddressedPeer.getParser());
                output.add(new PeerAccessReply(peers));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append("[numOfPeers=").append(Integer.toString(this.peers.size()))
                .append(']').toString();
    }

    @Override
    public int hashCode() {
        return this.peers.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof PeerAccessReply)) {
            return false;
        }
        final PeerAccessReply other = (PeerAccessReply) obj;
        return this.peers.equals(other.peers);
    }

}
