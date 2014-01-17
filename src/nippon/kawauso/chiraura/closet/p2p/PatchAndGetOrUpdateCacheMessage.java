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
final class PatchAndGetOrUpdateCacheMessage<T extends Mountain> implements Message {

    private final boolean get;

    private final long type;
    private final Chunk.Id<T> id;
    private final Mountain.Dust<T> diff;

    private final long date;

    private PatchAndGetOrUpdateCacheMessage(final boolean get, final long idType, final Chunk.Id<T> id, final Mountain.Dust<T> diff,
            final long date) {
        if (diff == null) {
            throw new IllegalArgumentException("Null diff.");
        }
        this.get = get;
        this.type = idType;
        this.id = id;
        this.diff = diff;
        this.date = date;
    }

    PatchAndGetOrUpdateCacheMessage(final TypeRegistry<Chunk.Id<?>> idRegistry, final Chunk.Id<T> id, final Mountain.Dust<T> diff) {
        this(true, idRegistry.getId(id), id, diff, 0);
    }

    PatchAndGetOrUpdateCacheMessage(final TypeRegistry<Chunk.Id<?>> idRegistry, final Chunk.Id<T> id, final Mountain.Dust<T> diff, final long date) {
        this(false, idRegistry.getId(id), id, diff, date);
    }

    boolean isGet() {
        return this.get;
    }

    Chunk.Id<T> getId() {
        return this.id;
    }

    Mountain.Dust<T> getDiff() {
        return this.diff;
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
            return 1 + BytesConversion.byteSize("loo", this.type, this.id, this.diff);
        } else {
            return 1 + BytesConversion.byteSize("lool", this.type, this.id, this.diff, this.date);
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.get) {
            output.write(1);
            return 1 + BytesConversion.toStream(output, "loo", this.type, this.id, this.diff);
        } else {
            output.write(0);
            return 1 + BytesConversion.toStream(output, "lool", this.type, this.id, this.diff, this.date);
        }
    }

    static BytesConvertible.Parser<PatchAndGetOrUpdateCacheMessage<?>> getParser(final TypeRegistry<Chunk.Id<?>> idRegistry,
            final TypeRegistry<Mountain.Dust<?>> diffRegistry) {
        return new BytesConvertible.Parser<PatchAndGetOrUpdateCacheMessage<?>>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super PatchAndGetOrUpdateCacheMessage<?>> output)
                    throws MyRuleException, IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                int size = 1;
                if (flag[0] == 1) {
                    size += get(input, maxByteSize - size, output);
                } else {
                    size += diff(input, maxByteSize - size, output);
                }
                return size;
            }

            private <T extends Mountain> int get(final InputStream input, final int maxByteSize, final List<? super PatchAndGetOrUpdateCacheMessage<T>> output)
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
                output.add(new PatchAndGetOrUpdateCacheMessage<>(true, type[0], id.get(0), diff.get(0), 0));
                return size;
            }

            private <T extends Mountain> int diff(final InputStream input, final int maxByteSize, final List<? super PatchAndGetOrUpdateCacheMessage<T>> output)
                    throws MyRuleException, IOException {
                final long[] type = new long[1];
                final List<Chunk.Id<T>> id = new ArrayList<>(1);
                final List<Mountain.Dust<T>> diff = new ArrayList<>(1);
                final long[] accessDate = new long[1];

                int size = NumberBytesConversion.fromStream(input, maxByteSize, type);
                final BytesConvertible.Parser<? extends Chunk.Id<?>> idParser = idRegistry.getParser(type[0]);
                final BytesConvertible.Parser<? extends Mountain.Dust<?>> diffParser = diffRegistry.getParser(type[0]);
                if (idParser == null) {
                    throw new MyRuleException("Not registered chunk id type ( " + type[0] + " ).");
                } else if (diffParser == null) {
                    throw new MyRuleException("Not registered diff type ( " + type[0] + " ).");
                }
                size += BytesConversion.fromStream(input, maxByteSize - size, "ool", id, idParser, diff, diffParser, accessDate);
                output.add(new PatchAndGetOrUpdateCacheMessage<>(false, type[0], id.get(0), diff.get(0), accessDate[0]));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id)
                .append(", ").append(this.diff);
        if (!this.get) {
            buff.append(", ").append(LoggingFunctions.getSimpleDate(this.date));
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.get ? 1231 : 1237);
        result = prime * result + this.id.hashCode();
        result = prime * result + this.diff.hashCode();
        result = prime * result + (int) (this.date ^ (this.date >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof PatchAndGetOrUpdateCacheMessage)) {
            return false;
        }
        final PatchAndGetOrUpdateCacheMessage<?> other = (PatchAndGetOrUpdateCacheMessage<?>) obj;
        return this.get == other.get && this.id.equals(other.id) && this.diff.equals(other.diff) && this.date == other.date;
    }

}
