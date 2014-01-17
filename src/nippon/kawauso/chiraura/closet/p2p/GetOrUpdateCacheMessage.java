/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class GetOrUpdateCacheMessage implements Message {

    private final boolean get;

    private final long type;
    private final Chunk.Id<? extends Mountain> id;

    private final long date;

    private GetOrUpdateCacheMessage(final boolean get, final long type, final Chunk.Id<? extends Mountain> id, final long date) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        }
        this.get = get;
        this.type = type;
        this.id = id;
        this.date = date;
    }

    GetOrUpdateCacheMessage(final TypeRegistry<Chunk.Id<?>> idRegistry, final Chunk.Id<? extends Mountain> id) {
        this(true, idRegistry.getId(id), id, 0);
    }

    GetOrUpdateCacheMessage(final TypeRegistry<Chunk.Id<?>> idRegistry, final Chunk.Id<? extends Mountain> id, final long date) {
        this(false, idRegistry.getId(id), id, date);
    }

    boolean isGet() {
        return this.get;
    }

    Chunk.Id<? extends Mountain> getId() {
        return this.id;
    }

    long getDate() {
        return this.date;
    }

    /*
     * 先頭バイトは、get のとき 1、そうでなければそれ以外。
     */

    @Override
    public int byteSize() {
        if (this.isGet()) {
            return 1 + BytesConversion.byteSize("lo", this.type, this.id);
        } else {
            return 1 + BytesConversion.byteSize("lol", this.type, this.id, this.date);
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.isGet()) {
            output.write(1);
            return 1 + BytesConversion.toStream(output, "lo", this.type, this.id);
        } else {
            output.write(0);
            return 1 + BytesConversion.toStream(output, "lol", this.type, this.id, this.date);
        }
    }

    static BytesConvertible.Parser<GetOrUpdateCacheMessage> getParser(final TypeRegistry<Chunk.Id<?>> idRegistry) {
        return new BytesConvertible.Parser<GetOrUpdateCacheMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super GetOrUpdateCacheMessage> output)
                    throws MyRuleException, IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);

                final long[] type = new long[1];
                final List<Chunk.Id<?>> id = new ArrayList<>(1);

                int size = 1;
                size += NumberBytesConversion.fromStream(input, maxByteSize, type);
                final BytesConvertible.Parser<? extends Chunk.Id<?>> parser = idRegistry.getParser(type[0]);
                if (parser == null) {
                    throw new MyRuleException("Not registered chunk id type ( " + type[0] + " ).");
                }
                size += parser.fromStream(input, maxByteSize - size, id);
                @SuppressWarnings("unchecked")
                final Chunk.Id<? extends Mountain> id0 = (Chunk.Id<? extends Mountain>) id.get(0);
                if (flag[0] == 1) {
                    output.add(new GetOrUpdateCacheMessage(true, type[0], id0, 0));
                } else {
                    final long[] date = new long[1];
                    size += NumberBytesConversion.fromStream(input, maxByteSize - size, date);
                    output.add(new GetOrUpdateCacheMessage(false, type[0], id0, date[0]));
                }
                return size;
            }
        };
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id);
        if (!this.get) {
            buff.append(", ").append(LoggingFunctions.getSimpleDate(this.date));
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + (this.get ? 1231 : 1237);
        result = prime * result + (int) (this.date ^ (this.date >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof GetOrUpdateCacheMessage)) {
            return false;
        }
        final GetOrUpdateCacheMessage other = (GetOrUpdateCacheMessage) obj;
        return this.get == other.get && this.id.equals(other.id) && this.date == other.date;
    }

}
