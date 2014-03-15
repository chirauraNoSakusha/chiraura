package nippon.kawauso.chiraura.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;

/**
 * @author chirauraNoSakusha
 */
final class SimpleIndex implements Storage.Index, BytesConvertible {

    private final Chunk.Id<?> id;
    private final long date;
    private final HashValue hashValue;

    SimpleIndex(final Chunk.Id<?> id, final long date, final HashValue hashValue) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        } else if (hashValue == null) {
            throw new IllegalArgumentException("Null hashValue.");
        }
        this.id = id;
        this.date = date;
        this.hashValue = hashValue;
    }

    SimpleIndex(final Chunk chunk) {
        this(chunk.getId(), chunk.getDate(), chunk.getHashValue());
    }

    @Override
    public Chunk.Id<?> getId() {
        return this.id;
    }

    @Override
    public long getDate() {
        return this.date;
    }

    @Override
    public HashValue getHashValue() {
        return this.hashValue;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("olo", this.id, this.date, this.hashValue);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "olo", this.id, this.date, this.hashValue);
    }

    static BytesConvertible.Parser<SimpleIndex> getParser(final BytesConvertible.Parser<? extends Chunk.Id<?>> parser) {
        return new BytesConvertible.Parser<SimpleIndex>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super SimpleIndex> output) throws MyRuleException, IOException {
                final List<Chunk.Id<?>> id = new ArrayList<>(1);
                final long[] date = new long[1];
                final List<HashValue> hashValue = new ArrayList<>(1);
                final int size = BytesConversion.fromStream(input, maxByteSize, "olo", id, parser, date, hashValue, HashValue.getParser());
                output.add(new SimpleIndex(id.get(0), date[0], hashValue.get(0)));
                return size;
            }
        };
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + (int) (this.date ^ (this.date >>> 32));
        result = prime * result + this.hashValue.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof SimpleIndex)) {
            return false;
        }
        final SimpleIndex other = (SimpleIndex) obj;
        return this.id.equals(other.id) && this.date == other.date && this.hashValue.equals(other.hashValue);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id)
                .append(", ").append(LoggingFunctions.getSimpleDate(this.date))
                .append(", ").append(this.hashValue)
                .append(']').toString();
    }

}
