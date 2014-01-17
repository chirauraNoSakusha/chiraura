/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 与えられた鍵で暗号化する封筒。
 * 暗号化後のバイト列が内部で保存される。
 * @author chirauraNoSakusha
 */
abstract class SkeletalEncryptedEnvelope<T extends Envelope> implements Envelope {

    private final T base;
    private final byte[] encrypted;

    protected SkeletalEncryptedEnvelope(final Proxy<T> proxy) {
        if (proxy == null) {
            throw new IllegalArgumentException("Null proxy.");
        }
        this.base = proxy.base;
        this.encrypted = proxy.encrypted;
    }

    SkeletalEncryptedEnvelope(final T base, final Key encryptionKey) {
        if (base == null) {
            throw new IllegalArgumentException("Null base.");
        } else if (encryptionKey == null) {
            throw new IllegalArgumentException("Null encryption key.");
        }
        this.base = base;
        this.encrypted = CryptographicFunctions.encrypt(encryptionKey, BytesConversion.toBytes(this.base));
    }

    @Override
    public List<Message> getMail() {
        return this.base.getMail();
    }

    @Override
    public int byteSize() {
        return (new Proxy<>(this)).byteSize();
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return (new Proxy<>(this)).toStream(output);
    }

    protected static final class Proxy<T extends Envelope> implements BytesConvertible {

        private final T base;
        private final byte[] encrypted;

        private Proxy(final T base, final byte[] encrypted) {
            this.base = base;
            this.encrypted = encrypted;
        }

        /*
         * こっちなら整合性を崩さないので protected でいい。
         */
        protected Proxy(final SkeletalEncryptedEnvelope<T> original) {
            this(original.base, original.encrypted);
        }

        @Override
        public int byteSize() {
            return BytesConversion.byteSize("ab", this.encrypted);
        }

        @Override
        public int toStream(final OutputStream output) throws IOException {
            return BytesConversion.toStream(output, "ab", this.encrypted);
        }

        protected static <T extends Envelope> BytesConvertible.Parser<Proxy<T>> getParser(final Key decryptionKey, final BytesConvertible.Parser<T> parser) {
            if (decryptionKey == null) {
                throw new IllegalArgumentException("Null decryotion key.");
            } else if (parser == null) {
                throw new IllegalArgumentException("Null parser.");
            }

            return new BytesConvertible.Parser<Proxy<T>>() {
                @Override
                public int fromStream(final InputStream input, final int maxByteSize, final List<? super Proxy<T>> output)
                        throws MyRuleException, IOException {
                    final byte[][] encrypted = new byte[1][];
                    final int size = BytesConversion.fromStream(input, maxByteSize, "ab", (Object) encrypted);

                    // 開錠。
                    byte[] decrypted;
                    try {
                        decrypted = CryptographicFunctions.decrypt(decryptionKey, encrypted[0]);
                    } catch (final IllegalArgumentException e) {
                        throw new MyRuleException(e);
                    }
                    final List<T> base = new ArrayList<>(1);
                    final int baseSize = parser.fromStream(new ByteArrayInputStream(decrypted), decrypted.length, base);
                    if (baseSize != decrypted.length) {
                        throw new MyRuleException("Different base size ( " + baseSize + " ) from expected size ( " + decrypted.length + " ).");
                    }

                    try {
                        output.add(new Proxy<>(base.get(0), encrypted[0]));
                    } catch (final IllegalArgumentException e) {
                        throw new MyRuleException(e);
                    }
                    return size;
                }
            };
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.encrypted);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SkeletalEncryptedEnvelope)) {
            return false;
        }
        final SkeletalEncryptedEnvelope<?> other = (SkeletalEncryptedEnvelope<?>) obj;
        return this.base.equals(other.base) && Arrays.equals(this.encrypted, other.encrypted);
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName())).append("[{");
        for (int i = 0; i < this.base.getMail().size(); i++) {
            if (i != 0) {
                buff.append(", ");
            }
            buff.append(this.base.getMail().get(i));
        }
        return buff.append("}]").toString();
    }

}
