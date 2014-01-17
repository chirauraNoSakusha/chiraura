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

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.cell.Utf8Cell;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;

/**
 * 名前を識別子とする可変データ片。
 * 可変と言いつつインスタンス自体は不変。
 * 同じ識別子、違う内容でデータ片を作成して置き換えることでデータ片を書き換える。
 * @author chirauraNoSakusha
 */
public final class VariableChunk extends SkeletalChunk {

    /**
     * 名前による識別子。
     * @author chirauraNoSakusha
     */
    public static final class Id implements Chunk.Id<VariableChunk> {

        private final String name;

        private final Address address;

        Id(final String name) {
            if (name == null) {
                throw new IllegalArgumentException("Null name.");
            }
            this.name = name;
            this.address = new Address(HashValue.calculateFromBytes(this.name.getBytes(Global.INTERNAL_CHARSET)).toBigInteger(), HashValue.SIZE);
        }

        @Override
        public Class<VariableChunk> getChunkClass() {
            return VariableChunk.class;
        }

        @Override
        public Address getAddress() {
            return this.address;
        }

        String getName() {
            return this.name;
        }

        @Override
        public int byteSize() {
            return (new Utf8Cell(this.name)).byteSize();
        }

        @Override
        public int toStream(final OutputStream output) throws IOException {
            return (new Utf8Cell(this.name)).toStream(output);
        }

        /**
         * 復号器を得る。
         * @return 復号器
         */
        public static BytesConvertible.Parser<Id> getParser() {
            return new BytesConvertible.Parser<Id>() {
                @Override
                public int fromStream(final InputStream input, final int maxByteSize, final List<? super Id> output) throws MyRuleException, IOException {
                    final List<Utf8Cell> name = new ArrayList<>(1);
                    final int size = Utf8Cell.getParser().fromStream(input, maxByteSize, name);
                    output.add(new Id(name.get(0).get()));
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
            } else if (!(obj instanceof Id)) {
                return false;
            }
            final Id other = (Id) obj;
            return this.name.equals(other.name);
        }

        @Override
        public String toString() {
            return (new StringBuilder(VariableChunk.class.getSimpleName())).append('.').append(this.getClass().getSimpleName())
                    .append('[').append(this.name)
                    .append(']').toString();
        }

    }

    private final Id id;
    private final long date;
    private final byte[] body;

    private HashValue hashValue;

    private VariableChunk(final Id id, final long date, final byte[] body, final HashValue hashValue) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        } else if (body == null) {
            throw new IllegalArgumentException("Null body.");
        }
        this.id = id;
        this.date = date;
        this.body = body;
        this.hashValue = hashValue;
    }

    VariableChunk(final String name, final long date, final byte[] body) {
        this(new Id(name), date, body, null);
    }

    /*
     * コピーコンストラクタは要らない。
     * なぜなら、不変だから。 body があれだけど、それは無視する。
     */

    @Override
    public Id getId() {
        return this.id;
    }

    /**
     * 名前を返す。
     * @return 名前
     */
    public String getName() {
        return this.id.name;
    }

    @Override
    public long getDate() {
        return this.date;
    }

    /**
     * 内容を返す。
     * 返す内容はコピーとかしてないので、書き変えないように。
     * @return 内容
     */
    byte[] getBody() {
        return this.body;
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
    public static BytesConvertible.Parser<VariableChunk> getParser() {
        return new BytesConvertible.Parser<VariableChunk>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super VariableChunk> output) throws MyRuleException,
                    IOException {
                final List<Id> id = new ArrayList<>(1);
                final long[] date = new long[1];
                final byte[][] body = new byte[1][];
                final int size = BytesConversion.fromStream(input, maxByteSize, "olab", id, Id.getParser(), date, body);
                final VariableChunk instance = new VariableChunk(id.get(0).name, date[0], body[0]);
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
        } else if (!(obj instanceof VariableChunk)) {
            return false;
        }
        final VariableChunk other = (VariableChunk) obj;
        return this.id.equals(other.id) && this.date == other.date && Arrays.equals(this.body, other.body);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id)
                .append(", ").append(LoggingFunctions.getSimpleDate(this.date))
                .append(", size=").append(this.body.length)
                .append(']').toString();
    }

}
