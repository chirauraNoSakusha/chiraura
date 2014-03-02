/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 通信鍵の更新を示す言付け。
 * @author chirauraNoSakusha
 */
final class KeyUpdateMessage implements Message {

    private final byte[] encryptedNewKey;
    private final byte[] signedHashValue;

    private KeyUpdateMessage(final byte[] encryptedNewKey, final byte[] signedHashValue) {
        if (encryptedNewKey == null) {
            throw new IllegalArgumentException("Null encryption key.");
        } else if (signedHashValue == null) {
            throw new IllegalArgumentException("Null signed hash value.");
        }
        this.encryptedNewKey = encryptedNewKey;
        this.signedHashValue = signedHashValue;
    }

    static KeyUpdateMessage newInstance(final PublicKey encryptionKey, final PrivateKey signKey, final Key newKey) {
        final byte[] keyBytes = newKey.getEncoded();
        return new KeyUpdateMessage(CryptographicFunctions.encrypt(encryptionKey, keyBytes),
                CryptographicFunctions.encrypt(signKey, BytesConversion.toBytes(HashValue.calculateFromBytes(keyBytes))));
    }

    Key getKey(final PrivateKey decryptionKey, final PublicKey confirmKey) throws MyRuleException {
        final byte[] newKeyBytes = CryptographicFunctions.decrypt(decryptionKey, this.encryptedNewKey);
        final Key newKey = CryptographicKeys.getCommonKey(newKeyBytes);
        final HashValue hashValue = BytesConversion.fromBytes(CryptographicFunctions.decrypt(confirmKey, this.signedHashValue), HashValue.getParser());
        if (!HashValue.calculateFromBytes(newKeyBytes).equals(hashValue)) {
            throw new MyRuleException("Invalid key or key hash value.");
        }

        return newKey;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("abab", this.encryptedNewKey, this.signedHashValue);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "abab", this.encryptedNewKey, this.signedHashValue);
    }

    static BytesConvertible.Parser<KeyUpdateMessage> getParser() {
        return new BytesConvertible.Parser<KeyUpdateMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super KeyUpdateMessage> output) throws MyRuleException,
                    IOException {
                final byte[][] encryptedNewKey = new byte[1][];
                final byte[][] signedHashValue = new byte[1][];
                final int size = BytesConversion.fromStream(input, maxByteSize, "abab", encryptedNewKey, signedHashValue);
                output.add(new KeyUpdateMessage(encryptedNewKey[0], signedHashValue[0]));
                return size;
            }
        };
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.encryptedNewKey);
        result = prime * result + Arrays.hashCode(this.signedHashValue);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof KeyUpdateMessage)) {
            return false;
        }
        final KeyUpdateMessage other = (KeyUpdateMessage) obj;
        return Arrays.equals(this.encryptedNewKey, other.encryptedNewKey) && Arrays.equals(this.signedHashValue, other.signedHashValue);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append("[encryptedNewKeySize=").append(this.encryptedNewKey.length)
                .append(", signedHashValueSize=").append(this.signedHashValue.length)
                .append(']').toString();
    }
}
