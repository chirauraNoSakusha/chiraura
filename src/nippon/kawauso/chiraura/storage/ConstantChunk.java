/**
 * 
 */
package nippon.kawauso.chiraura.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;

/**
 * データ片の内容のハッシュ値を識別子とする不変データ片。
 * 識別子が等しければ、データ片の内容も等しいとみなされる。ただし、名前や最終更新日時は違っていてもいい。
 * 識別子が等しくても、内容の違うデータ片がある場合、その中のどれが生き残るかは何の保証もされない。
 * @author chirauraNoSakusha
 */
public final class ConstantChunk extends SkeletalChunk {

    /**
     * データ片の内容のハッシュ値による識別子。
     * @author chirauraNoSakusha
     */
    public static final class Id implements Chunk.Id<ConstantChunk> {
        private final Address address;

        Id(final HashValue hashValue) {
            if (hashValue == null) {
                throw new IllegalArgumentException("Null hashValue.");
            }
            this.address = new Address(hashValue.toBigInteger(), HashValue.SIZE);
        }

        @Override
        public Class<ConstantChunk> getChunkClass() {
            return ConstantChunk.class;
        }

        @Override
        public Address getAddress() {
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

        /**
         * 復号器を得る。
         * @return 復号器
         */
        public static BytesConvertible.Parser<Id> getParser() {
            return new BytesConvertible.Parser<Id>() {
                @Override
                public int fromStream(final InputStream input, final int maxByteSize, final List<? super Id> output) throws MyRuleException, IOException {
                    final List<HashValue> signature = new ArrayList<>(1);
                    final int size = HashValue.getParser().fromStream(input, maxByteSize, signature);
                    output.add(new Id(signature.get(0)));
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
            if (!(obj instanceof Id)) {
                return false;
            }
            final Id other = (Id) obj;
            return this.address.equals(other.address);
        }

        @Override
        public String toString() {
            return (new StringBuilder(ConstantChunk.class.getSimpleName())).append('.').append(this.getClass().getSimpleName())
                    .append('[').append(this.address)
                    .append(']').toString();
        }
    }

    private final Id id;
    private final long date;
    private final byte[] body;

    private HashValue hashValue;

    ConstantChunk(final long date, final byte[] body) {
        if (body == null) {
            throw new IllegalArgumentException("Null body.");
        }

        this.date = date;
        this.body = body;
        this.id = new Id(HashValue.calculateFromBytes(this.body));

        this.hashValue = null;
    }

    /*
     * コピーコンストラクタは要らない。
     * なぜなら、不変だから。 body があれだけど、それは無視する。
     */

    @Override
    public Id getId() {
        return this.id;
    }

    @Override
    public long getDate() {
        return this.date;
    }

    @Override
    public HashValue getHashValue() {
        if (this.hashValue == null) {
            this.hashValue = super.getHashValue();
        }
        return this.hashValue;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("olab", this.id, this.date, this.body);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "olab", this.id, this.date, this.body);
    }

    /**
     * 復号器を得る。
     * @return 復号器
     */
    public static BytesConvertible.Parser<ConstantChunk> getParser() {
        return new BytesConvertible.Parser<ConstantChunk>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super ConstantChunk> output) throws MyRuleException,
                    IOException {
                final List<Id> id = new ArrayList<>(1);
                final long[] date = new long[1];
                final byte[][] body = new byte[1][];
                final int size = BytesConversion.fromStream(input, maxByteSize, "olab", id, Id.getParser(), date, body);
                final ConstantChunk instance = new ConstantChunk(date[0], body[0]);

                // 整合性検査。
                if (!id.get(0).equals(instance.id)) {
                    throw new MyRuleException("Reproduced id ( " + instance.id + " ) differes parsed id ( " + id.get(0) + " ).");
                }

                output.add(instance);
                return size;
            }
        };
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof ConstantChunk)) {
            return false;
        }
        final ConstantChunk other = (ConstantChunk) obj;
        return this.id.equals(other.id) && this.date == other.date && Arrays.equals(this.body, other.body);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append("[").append(this.id)
                .append(", ").append(LoggingFunctions.getSimpleDate(this.date))
                .append(", size=").append(this.body.length)
                .append(']').toString();
    }

}
