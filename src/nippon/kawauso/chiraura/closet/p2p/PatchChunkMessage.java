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
final class PatchChunkMessage<T extends Mountain> implements Message {

    private final long type;
    private final Chunk.Id<T> id;
    private final Mountain.Dust<T> diff;

    private PatchChunkMessage(final long type, final Chunk.Id<T> id, final Mountain.Dust<T> diff) {
        if (id == null) {
            throw new IllegalArgumentException("Null chunk.");
        } else if (diff == null) {
            throw new IllegalArgumentException("Null diff.");
        }
        this.type = type;
        this.id = id;
        this.diff = diff;
    }

    PatchChunkMessage(final TypeRegistry<Chunk.Id<?>> idRegistry, final Chunk.Id<T> id, final Mountain.Dust<T> diff) {
        this(idRegistry.getId(id), id, diff);
    }

    Chunk.Id<T> getId() {
        return this.id;
    }

    Mountain.Dust<T> getDiff() {
        return this.diff;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("loo", this.type, this.id, this.diff);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "loo", this.type, this.id, this.diff);
    }

    static BytesConvertible.Parser<PatchChunkMessage<?>> getParser(final TypeRegistry<Chunk.Id<?>> idRegistry, final TypeRegistry<Mountain.Dust<?>> diffRegistry) {
        return new BytesConvertible.Parser<PatchChunkMessage<?>>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super PatchChunkMessage<?>> output) throws MyRuleException,
                    IOException {
                return subFromStream(input, maxByteSize, output);
            }

            private <T extends Mountain> int subFromStream(final InputStream input, final int maxByteSize, final List<? super PatchChunkMessage<T>> output)
                    throws MyRuleException, IOException {
                final long[] type = new long[1];
                final List<Chunk.Id<T>> id = new ArrayList<>(1);
                final List<Mountain.Dust<T>> diff = new ArrayList<>(1);

                int size = NumberBytesConversion.fromStream(input, maxByteSize, type);
                final BytesConvertible.Parser<? extends Chunk.Id<?>> idParser = idRegistry.getParser(type[0]);
                final BytesConvertible.Parser<? extends Mountain.Dust<?>> diffParser = diffRegistry.getParser(type[0]);
                if (idParser == null) {
                    throw new MyRuleException("Not registered chunk id type ( " + type[0] + " ).");
                } else if (diffParser == null) {
                    throw new MyRuleException("Not registered diff type ( " + type[0] + " ).");
                }
                size += BytesConversion.fromStream(input, maxByteSize - size, "oo", id, idParser, diff, diffParser);
                output.add(new PatchChunkMessage<>(type[0], id.get(0), diff.get(0)));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id)
                .append(", ").append(this.diff)
                .append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + this.diff.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof PatchChunkMessage)) {
            return false;
        }
        final PatchChunkMessage<?> other = (PatchChunkMessage<?>) obj;
        return this.diff.equals(other.diff) && this.id.equals(other.id);
    }

}
