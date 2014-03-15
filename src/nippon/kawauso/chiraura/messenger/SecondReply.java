package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nippon.kawauso.chiraura.lib.connection.PeerCell;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
final class SecondReply implements Message {

    private final PublicKey id;
    private final byte[] encryptedWatchword;
    private final PublicKey key;
    private final long version;
    private final InetSocketAddress peer;

    private SecondReply(final PublicKey id, final byte[] encryptedWatchword, final PublicKey key, final long version, final InetSocketAddress peer) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        } else if (encryptedWatchword == null) {
            throw new IllegalArgumentException("Null encrypted watchword.");
        } else if (key == null) {
            throw new IllegalArgumentException("Null key.");
        } else if (peer == null) {
            throw new IllegalArgumentException("Null peer.");
        }
        this.id = id;
        this.encryptedWatchword = encryptedWatchword;
        this.key = key;
        this.version = version;
        this.peer = peer;
    }

    /**
     * 作成する。
     * @param id 識別用鍵
     * @param watchword 署名用バイト列
     * @param key 通信用公開鍵
     * @param type 接続種別
     * @param destination こっちから見える通信相手
     */
    SecondReply(final KeyPair id, final byte[] watchword, final PublicKey key, final long version, final InetSocketAddress destination) {
        this(id.getPublic(), CryptographicFunctions.encrypt(id.getPrivate(), watchword), key, version, destination);
    }

    PublicKey getId() {
        return this.id;
    }

    byte[] getEncryptedWatchword() {
        return this.encryptedWatchword;
    }

    PublicKey getKey() {
        return this.key;
    }

    long getVersion() {
        return this.version;
    }

    InetSocketAddress getPeer() {
        return this.peer;
    }

    @Override
    public int byteSize() {
        return BytesConversion
                .byteSize("ababablo", this.id.getEncoded(), this.encryptedWatchword, this.key.getEncoded(), this.version, new PeerCell(this.peer));
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "ababablo", this.id.getEncoded(), this.encryptedWatchword, this.key.getEncoded(), this.version, new PeerCell(
                this.peer));
    }

    static BytesConvertible.Parser<SecondReply> getParser() {
        return new BytesConvertible.Parser<SecondReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super SecondReply> output) throws MyRuleException, IOException {
                final byte[][] id = new byte[1][];
                final byte[][] encryptedWatchword = new byte[1][];
                final byte[][] key = new byte[1][];
                final long[] version = new long[1];
                final List<PeerCell> peer = new ArrayList<>(1);
                final int size = BytesConversion.fromStream(input, maxByteSize, "ababablo", id, encryptedWatchword, key, version, peer, PeerCell.getParser());
                try {
                    output.add(new SecondReply(CryptographicKeys.getPublicKey(id[0]), encryptedWatchword[0], CryptographicKeys.getPublicKey(key[0]),
                            version[0], peer.get(0).get()));
                } catch (final IllegalArgumentException e) {
                    throw new MyRuleException(e);
                }
                return size;
            }
        };
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + Arrays.hashCode(this.encryptedWatchword);
        result = prime * result + this.key.hashCode();
        result = prime * result + (int) (this.version ^ (this.version >>> 32));
        result = prime * result + this.peer.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof SecondReply)) {
            return false;
        }
        final SecondReply other = (SecondReply) obj;
        return this.id.equals(other.id) && Arrays.equals(this.encryptedWatchword, other.encryptedWatchword) && this.key.equals(other.key)
                && this.version == other.version && this.peer.equals(other.peer);
    }
}
