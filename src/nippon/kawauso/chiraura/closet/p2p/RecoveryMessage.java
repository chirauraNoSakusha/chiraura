package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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
final class RecoveryMessage implements Message {

    private final boolean get;

    private final long type;
    private final Chunk.Id<?> id;

    private final long date;

    private RecoveryMessage(final boolean get, final long type, final Chunk.Id<?> id, final long date) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        }
        this.get = get;
        this.type = type;
        this.id = id;
        this.date = date;
    }

    RecoveryMessage(final TypeRegistry<Chunk.Id<?>> idRegistry, final Chunk.Id<?> id) {
        this(true, idRegistry.getId(id), id, 0);
    }

    RecoveryMessage(final TypeRegistry<Chunk.Id<?>> idRegistry, final Chunk.Id<?> id, final long date) {
        this(false, idRegistry.getId(id), id, date);
    }

    boolean isGet() {
        return this.get;
    }

    Chunk.Id<?> getId() {
        return this.id;
    }

    long getDate() {
        return this.date;
    }

    /*
     * 先頭バイトは、Get なら 1、そうでないならそれ以外。
     */

    @Override
    public int byteSize() {
        if (this.get) {
            return 1 + BytesConversion.byteSize("lo", this.type, this.id);
        } else {
            return 1 + BytesConversion.byteSize("lol", this.type, this.id, this.date);
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.get) {
            output.write(1);
            return 1 + BytesConversion.toStream(output, "lo", this.type, this.id);
        } else {
            output.write(0);
            return 1 + BytesConversion.toStream(output, "lol", this.type, this.id, this.date);
        }
    }

    static BytesConvertible.Parser<RecoveryMessage> getParser(final TypeRegistry<Chunk.Id<?>> idRegistry) {
        return new BytesConvertible.Parser<RecoveryMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super RecoveryMessage> output) throws MyRuleException,
                    IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                final long[] type = new long[1];
                final List<Chunk.Id<?>> id = new ArrayList<>(1);
                int size = 1 + NumberBytesConversion.fromStream(input, maxByteSize, type);
                final BytesConvertible.Parser<? extends Chunk.Id<?>> parser = idRegistry.getParser(type[0]);
                if (parser == null) {
                    throw new MyRuleException("Not registered chunk id type ( " + type[0] + " ).");
                }
                size += parser.fromStream(input, maxByteSize - size, id);
                if (flag[0] == 1) {
                    output.add(new RecoveryMessage(true, type[0], id.get(0), 0));
                } else {
                    final long[] date = new long[1];
                    size += NumberBytesConversion.fromStream(input, maxByteSize, date);
                    output.add(new RecoveryMessage(false, type[0], id.get(0), date[0]));
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
        result = prime * result + (int) (this.date ^ (this.date >>> 32));
        result = prime * result + (this.get ? 1231 : 1237);
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof RecoveryMessage)) {
            return false;
        }
        final RecoveryMessage other = (RecoveryMessage) obj;
        return this.get == other.get && this.id.equals(other.id) && this.date == other.date;
    }

}
