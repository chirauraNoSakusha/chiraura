package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PublicKey;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 一言目。
 * @author chirauraNoSakusha
 */
final class FirstMessage implements Message {

    private final PublicKey key;

    /**
     * 作成する。
     * @param key 通信用公開鍵
     */
    FirstMessage(final PublicKey key) {
        if (key == null) {
            throw new IllegalArgumentException("Null key.");
        }
        this.key = key;
    }

    PublicKey getKey() {
        return this.key;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("ab", this.key.getEncoded());
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "ab", this.key.getEncoded());
    }

    static BytesConvertible.Parser<FirstMessage> getParser() {
        return new BytesConvertible.Parser<FirstMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super FirstMessage> output) throws MyRuleException, IOException {
                final byte[][] key = new byte[1][];
                final int size = BytesConversion.fromStream(input, maxByteSize, "ab", (Object) key);
                try {
                    output.add(new FirstMessage(CryptographicKeys.getPublicKey(key[0])));
                } catch (final IllegalArgumentException e) {
                    throw new MyRuleException(e);
                }
                return size;
            }
        };
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof FirstMessage)) {
            return false;
        }
        final FirstMessage other = (FirstMessage) obj;
        return this.key.equals(other.key);
    }

}
