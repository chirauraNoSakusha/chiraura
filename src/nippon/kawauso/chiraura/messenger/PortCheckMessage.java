package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
final class PortCheckMessage implements Message {

    private final byte[] encryptedKey;

    private PortCheckMessage(final byte[] encryptedKey) {
        this.encryptedKey = encryptedKey;
    }

    PortCheckMessage(final PublicKey destinationId, final Key key) {
        this(CryptographicFunctions.encrypt(destinationId, key.getEncoded()));
    }

    byte[] getEncryptedKey() {
        return this.encryptedKey;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("ab", this.encryptedKey);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "ab", this.encryptedKey);
    }

    static BytesConvertible.Parser<PortCheckMessage> getParser() {
        return new BytesConvertible.Parser<PortCheckMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super PortCheckMessage> output) throws MyRuleException,
                    IOException {
                final byte[][] encryptedKey = new byte[1][];
                final int size = BytesConversion.fromStream(input, maxByteSize, "ab", (Object) encryptedKey);
                output.add(new PortCheckMessage(encryptedKey[0]));
                return size;
            }
        };
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.encryptedKey);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof PortCheckMessage)) {
            return false;
        }
        final PortCheckMessage other = (PortCheckMessage) obj;
        return Arrays.equals(this.encryptedKey, other.encryptedKey);
    }

}
