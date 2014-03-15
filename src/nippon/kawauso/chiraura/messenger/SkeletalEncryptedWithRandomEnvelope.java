package nippon.kawauso.chiraura.messenger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.spec.SecretKeySpec;

import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 与えられた鍵と即席の乱数で暗号化する封筒。
 * 暗号化後のバイト列が内部で保存される。
 * @author chirauraNoSakusha
 */
abstract class SkeletalEncryptedWithRandomEnvelope<T extends Envelope> extends SkeletalEncryptedEnvelope<T> {

    private final byte[] encodedKey;

    protected SkeletalEncryptedWithRandomEnvelope(final Proxy<T> proxy) {
        super(proxy.base);
        this.encodedKey = proxy.encodedKey;
    }

    /*
     * 引数変換用。
     * 全く、一行目からしか呼べないし、抽象クラスはファクトリも作れないし、まるで地獄だぜ。
     */
    private SkeletalEncryptedWithRandomEnvelope(final T base, final byte[] givenKey, final byte[] randomKey, final String algorithm) {
        super(base, new SecretKeySpec(compositeBytes(givenKey, randomKey), algorithm));
        this.encodedKey = encodeBytes(randomKey);
    }

    /*
     * 引数変換用。
     * 全く(ry
     */
    private SkeletalEncryptedWithRandomEnvelope(final T base, final byte[] givenKey, final String algorithm) {
        this(base, givenKey, newRandomBytes(givenKey.length), algorithm);
    }

    SkeletalEncryptedWithRandomEnvelope(final T base, final Key encryptionKey) {
        this(base, encryptionKey.getEncoded(), encryptionKey.getAlgorithm());
    }

    /**
     * ランダムバイト列を作る。
     * @param size バイト数
     * @return ランダムバイト列
     */
    private static byte[] newRandomBytes(final int size) {
        final byte[] bytes = new byte[size];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

    /**
     * バイト列を合成する。
     * @param bytes1 合成するバイト列
     * @param bytes2 合成するバイト列
     * @return 合成したバイト列。
     *         長さは短い方に合わせられる
     */
    private static byte[] compositeBytes(final byte[] bytes1, final byte[] bytes2) {
        final byte[] result = new byte[Math.min(bytes1.length, bytes2.length)];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (bytes1[i] ^ bytes2[i]);
        }
        return result;
    }

    /**
     * バイト列に細工する。
     * @param before 細工前のバイト列
     * @return 細工後のバイト列
     */
    private static byte[] encodeBytes(final byte[] before) {
        /*
         * 何と言うことでしょう。
         * 元のバイトを1つ、2つ、3つ、...と挟むようにランダムバイトが加えられています。
         */
        final Random random = ThreadLocalRandom.current();
        final ByteArrayOutputStream buff = new ByteArrayOutputStream(before.length + 10);
        for (int i = 1, index = 0; index < before.length; i++) {
            buff.write(random.nextInt(256));
            for (int j = 0; j < i && index < before.length; j++) {
                buff.write(before[index]);
                index++;
            }
        }
        return buff.toByteArray();
    }

    /**
     * バイト列の細工を外す。
     * @param after 細工後のバイト列
     * @return 細工前のバイト列
     */
    private static byte[] decodeBytes(final byte[] after) {
        final ByteArrayOutputStream buff = new ByteArrayOutputStream(after.length);
        for (int i = 1, index = 0; index < after.length; i++) {
            index++;
            for (int j = 0; j < i && index < after.length; j++) {
                buff.write(after[index]);
                index++;
            }
        }
        return buff.toByteArray();
    }

    @Override
    public int byteSize() {
        return (new Proxy<>(this)).byteSize();
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return (new Proxy<>(this)).toStream(output);
    }

    static final class Proxy<T extends Envelope> implements BytesConvertible {

        private final SkeletalEncryptedEnvelope.Proxy<T> base;
        private final byte[] encodedKey;

        private Proxy(final SkeletalEncryptedEnvelope.Proxy<T> base, final byte[] encodedKey) {
            this.base = base;
            this.encodedKey = encodedKey;
        }

        private Proxy(final SkeletalEncryptedWithRandomEnvelope<T> original) {
            this.base = new SkeletalEncryptedEnvelope.Proxy<>(original);
            this.encodedKey = original.encodedKey;
        }

        @Override
        public int byteSize() {
            return BytesConversion.byteSize("ab", this.encodedKey) + this.base.byteSize();
        }

        @Override
        public int toStream(final OutputStream output) throws IOException {
            return BytesConversion.toStream(output, "ab", this.encodedKey) + this.base.toStream(output);
        }

        static <T extends Envelope> BytesConvertible.Parser<Proxy<T>> getParser(final Key decryptionKey, final BytesConvertible.Parser<T> parser) {
            if (decryptionKey == null) {
                throw new IllegalArgumentException("Null decryotion key.");
            } else if (parser == null) {
                throw new IllegalArgumentException("Null parser.");
            }

            return new BytesConvertible.Parser<Proxy<T>>() {
                @Override
                public int fromStream(final InputStream input, final int maxByteSize, final List<? super Proxy<T>> output) throws MyRuleException, IOException {
                    int size = 0;
                    final byte[][] encodedKey = new byte[1][];
                    size += BytesConversion.fromStream(input, maxByteSize, "ab", (Object) encodedKey);

                    // 暗号鍵の復元。
                    final byte[] randomKey = decodeBytes(encodedKey[0]);
                    final byte[] givenKey = decryptionKey.getEncoded();
                    if (givenKey.length != randomKey.length) {
                        throw new MyRuleException("Different key length ( " + randomKey.length + " ) from expected length ( " + givenKey.length + " ).");
                    }

                    final Key actualKey = new SecretKeySpec(compositeBytes(givenKey, randomKey), decryptionKey.getAlgorithm());
                    final List<SkeletalEncryptedEnvelope.Proxy<T>> base = new ArrayList<>(1);
                    size += SkeletalEncryptedEnvelope.Proxy.getParser(actualKey, parser).fromStream(input, maxByteSize - size, base);

                    output.add(new Proxy<>(base.get(0), encodedKey[0]));
                    return size;
                }
            };
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(this.encodedKey);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof SkeletalEncryptedWithRandomEnvelope)) {
            return false;
        } else if (!super.equals(obj)) {
            return false;
        }
        final SkeletalEncryptedWithRandomEnvelope<?> other = (SkeletalEncryptedWithRandomEnvelope<?>) obj;
        if (!Arrays.equals(this.encodedKey, other.encodedKey)) {
            return false;
        }
        return true;
    }

}
