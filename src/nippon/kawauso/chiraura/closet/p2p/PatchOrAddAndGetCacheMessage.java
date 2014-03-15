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
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class PatchOrAddAndGetCacheMessage implements Message {

    private final long type;
    private final Mountain chunk;

    private PatchOrAddAndGetCacheMessage(final long type, final Mountain chunk) {
        if (chunk == null) {
            throw new IllegalArgumentException("Null chunk.");
        }
        this.type = type;
        this.chunk = chunk;
    }

    PatchOrAddAndGetCacheMessage(final TypeRegistry<Chunk> chunkRegistry, final Mountain chunk) {
        this(chunkRegistry.getId(chunk), chunk);
    }

    Mountain getChunk() {
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

    static BytesConvertible.Parser<PatchOrAddAndGetCacheMessage> getParser(final TypeRegistry<Chunk> chunkRegistry) {
        return new BytesConvertible.Parser<PatchOrAddAndGetCacheMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super PatchOrAddAndGetCacheMessage> output)
                    throws MyRuleException, IOException {
                final long[] type = new long[1];
                final List<Mountain> chunk = new ArrayList<>(1);

                int size = NumberBytesConversion.fromStream(input, maxByteSize, type);
                final BytesConvertible.Parser<? extends Chunk> parser = chunkRegistry.getParser(type[0]);
                if (parser == null) {
                    throw new MyRuleException("Not registered chunk type ( " + type[0] + " ).");
                }
                size += BytesConversion.fromStream(input, maxByteSize - size, "o", chunk, parser);
                try {
                    output.add(new PatchOrAddAndGetCacheMessage(type[0], chunk.get(0)));
                } catch (final ClassCastException e) {
                    throw new MyRuleException(e);
                }
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
        return this.chunk.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof PatchOrAddAndGetCacheMessage)) {
            return false;
        }
        final PatchOrAddAndGetCacheMessage other = (PatchOrAddAndGetCacheMessage) obj;
        return this.chunk.equals(other.chunk);
    }

}
