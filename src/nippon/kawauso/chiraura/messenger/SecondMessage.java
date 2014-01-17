/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nippon.kawauso.chiraura.lib.connection.PeerCell;
import nippon.kawauso.chiraura.lib.connection.PortFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
final class SecondMessage implements Message {

    private final PublicKey id;
    private final byte[] encryptedKey;
    private final byte[] watchword;
    private final long version;
    private final int port;
    private final int type;
    private final InetSocketAddress peer;

    private SecondMessage(final PublicKey id, final byte[] encryptedKey, final byte[] watchword, final long version, final int port, final int type,
            final InetSocketAddress peer) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        } else if (encryptedKey == null) {
            throw new IllegalArgumentException("Null encrypted key.");
        } else if (watchword == null) {
            throw new IllegalArgumentException("Null watchword.");
        } else if (!PortFunctions.isValid(port)) {
            throw new IllegalArgumentException("invalid port.");
        } else if (peer == null) {
            throw new IllegalArgumentException("Null peer.");
        }
        this.id = id;
        this.encryptedKey = encryptedKey;
        this.watchword = watchword;
        this.version = version;
        this.port = port;
        this.type = type;
        this.peer = peer;
    }

    /**
     * 作成する。
     * @param id 識別用鍵
     * @param key 通信用共通鍵
     * @param watchword 署名用バイト列
     * @param version アプリケーションプロトコルバージョン
     * @param port 受け付けポート番号
     * @param type 接続種別
     * @param destination こっちから見える通信相手
     */
    SecondMessage(final KeyPair id, final Key key, final byte[] watchword, final long version, final int port, final int type,
            final InetSocketAddress destination) {
        this(id.getPublic(), CryptographicFunctions.encrypt(id.getPrivate(), key.getEncoded()), watchword, version, port, type, destination);
    }

    PublicKey getId() {
        return this.id;
    }

    byte[] getEncryptedKey() {
        return this.encryptedKey;
    }

    byte[] getWatchword() {
        return this.watchword;
    }

    long getVersion() {
        return this.version;
    }

    int getPort() {
        return this.port;
    }

    int getType() {
        return this.type;
    }

    InetSocketAddress getPeer() {
        return this.peer;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("abababliio", this.id.getEncoded(), this.encryptedKey, this.watchword, this.version, this.port, this.type,
                new PeerCell(this.peer));
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "abababliio", this.id.getEncoded(), this.encryptedKey, this.watchword, this.version, this.port, this.type,
                new PeerCell(this.peer));
    }

    static BytesConvertible.Parser<SecondMessage> getParser() {
        return new BytesConvertible.Parser<SecondMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super SecondMessage> output) throws MyRuleException, IOException {
                final byte[][] id = new byte[1][];
                final byte[][] encryptedKey = new byte[1][];
                final byte[][] watchword = new byte[1][];
                final long[] version = new long[1];
                final int[] port = new int[1];
                final int[] type = new int[1];
                final List<PeerCell> peer = new ArrayList<>(1);
                final int size = BytesConversion.fromStream(input, maxByteSize, "abababliio", id, encryptedKey, watchword, version, port, type, peer,
                        PeerCell.getParser());
                try {
                    output.add(new SecondMessage(CryptographicKeys.getPublicKey(id[0]), encryptedKey[0], watchword[0], version[0], port[0], type[0],
                            peer.get(0).get()));
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
        result = prime * result + Arrays.hashCode(this.encryptedKey);
        result = prime * result + Arrays.hashCode(this.watchword);
        result = prime * result + (int) (this.version ^ (this.version >>> 32));
        result = prime * result + this.port;
        result = prime * result + this.type;
        result = prime * result + this.peer.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof SecondMessage)) {
            return false;
        }
        final SecondMessage other = (SecondMessage) obj;
        return this.id.equals(other.id) && Arrays.equals(this.encryptedKey, other.encryptedKey) && Arrays.equals(this.watchword, other.watchword)
                && this.version == other.version && this.port == other.port && this.type == other.type && this.peer.equals(other.peer);
    }
}
