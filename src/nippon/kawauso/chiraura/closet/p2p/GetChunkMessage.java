/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class GetChunkMessage implements Message {

    private final long type;
    private final Chunk.Id<?> id;

    private GetChunkMessage(final long type, final Chunk.Id<?> id) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        }
        this.type = type;
        this.id = id;
    }

    GetChunkMessage(final TypeRegistry<Chunk.Id<?>> idRegistry, final Chunk.Id<?> id) {
        this(idRegistry.getId(id), id);
    }

    Chunk.Id<?> getId() {
        return this.id;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("lo", this.type, this.id);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "lo", this.type, this.id);
    }

    static BytesConvertible.Parser<GetChunkMessage> getParser(final TypeRegistry<Chunk.Id<?>> idRegistry) {
        return new BytesConvertible.Parser<GetChunkMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super GetChunkMessage> output) throws MyRuleException,
                    IOException {
                final long[] type = new long[1];
                final List<Chunk.Id<?>> id = new ArrayList<>(1);

                int size = NumberBytesConversion.fromStream(input, maxByteSize, type);
                final BytesConvertible.Parser<? extends Chunk.Id<?>> parser = idRegistry.getParser(type[0]);
                if (parser == null) {
                    throw new MyRuleException("Not registered chunk id type ( " + type[0] + " ).");
                }
                size += parser.fromStream(input, maxByteSize - size, id);
                output.add(new GetChunkMessage(type[0], id.get(0)));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id)
                .append(']').toString();
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof GetChunkMessage)) {
            return false;
        }
        final GetChunkMessage other = (GetChunkMessage) obj;
        return this.id.equals(other.id);
    }

}
