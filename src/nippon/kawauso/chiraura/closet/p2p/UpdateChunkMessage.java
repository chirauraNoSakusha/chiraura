package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.closet.Mountain;
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
final class UpdateChunkMessage implements Message {

    private final long type;
    private final Chunk.Id<? extends Mountain> id;
    private final long date;

    private UpdateChunkMessage(final long type, final Chunk.Id<? extends Mountain> id, final long date) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        }
        this.type = type;
        this.id = id;
        this.date = date;
    }

    UpdateChunkMessage(final TypeRegistry<Chunk.Id<?>> idRegistry, final Chunk.Id<? extends Mountain> id, final long date) {
        this(idRegistry.getId(id), id, date);
    }

    Chunk.Id<? extends Mountain> getId() {
        return this.id;
    }

    long getDate() {
        return this.date;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("lol", this.type, this.id, this.date);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "lol", this.type, this.id, this.date);
    }

    static BytesConvertible.Parser<UpdateChunkMessage> getParser(final TypeRegistry<Chunk.Id<?>> idRegistry) {
        return new BytesConvertible.Parser<UpdateChunkMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super UpdateChunkMessage> output) throws MyRuleException,
                    IOException {
                final long[] type = new long[1];
                final List<Chunk.Id<? extends Mountain>> id = new ArrayList<>(1);
                final long[] date = new long[1];

                int size = NumberBytesConversion.fromStream(input, maxByteSize, type);
                final BytesConvertible.Parser<? extends Chunk.Id<?>> parser = idRegistry.getParser(type[0]);
                if (parser == null) {
                    throw new MyRuleException("Not registered chunk id type ( " + type[0] + " ).");
                }
                size += BytesConversion.fromStream(input, maxByteSize - size, "ol", id, parser, date);
                output.add(new UpdateChunkMessage(type[0], id.get(0), date[0]));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id)
                .append(", ").append(LoggingFunctions.getSimpleDate(this.date))
                .append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + (int) (this.date ^ (this.date >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof UpdateChunkMessage)) {
            return false;
        }
        final UpdateChunkMessage other = (UpdateChunkMessage) obj;
        return this.id.equals(other.id) && this.date == other.date;
    }

}
