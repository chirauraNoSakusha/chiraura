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
final class AddChunkMessage implements Message {

    private final long type;
    private final Chunk chunk;

    private AddChunkMessage(final long type, final Chunk chunk) {
        if (chunk == null) {
            throw new IllegalArgumentException("Null chunk.");
        }
        this.type = type;
        this.chunk = chunk;
    }

    AddChunkMessage(final TypeRegistry<Chunk> chunkRegistry, final Chunk chunk) {
        this(chunkRegistry.getId(chunk), chunk);
    }

    Chunk getChunk() {
        return this.chunk;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("lo", this.type, this.chunk);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "lo", this.type, this.chunk);
    }

    static BytesConvertible.Parser<AddChunkMessage> getParser(final TypeRegistry<Chunk> chunkRegistry) {
        return new BytesConvertible.Parser<AddChunkMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super AddChunkMessage> output) throws MyRuleException,
                    IOException {
                final long[] type = new long[1];
                final List<Chunk> chunk = new ArrayList<>(1);

                int size = NumberBytesConversion.fromStream(input, maxByteSize, type);
                final BytesConvertible.Parser<? extends Chunk> parser = chunkRegistry.getParser(type[0]);
                if (parser == null) {
                    throw new MyRuleException("Not registered chunk type ( " + type[0] + " ).");
                }
                size += parser.fromStream(input, maxByteSize - size, chunk);
                output.add(new AddChunkMessage(type[0], chunk.get(0)));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.chunk.getId())
                .append(']').toString();
    }

    @Override
    public int hashCode() {
        return this.chunk.getId().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof AddChunkMessage)) {
            return false;
        }
        final AddChunkMessage other = (AddChunkMessage) obj;
        return this.chunk.equals(other.chunk);
    }

}
