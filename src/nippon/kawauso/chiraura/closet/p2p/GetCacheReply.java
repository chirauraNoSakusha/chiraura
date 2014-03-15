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
final class GetCacheReply implements Message {

    private final boolean rejected;
    private final boolean givenUp;

    private final long type;

    private final Chunk chunk;

    private final Chunk.Id<?> id;

    private final long accessDate;

    private GetCacheReply(final boolean rejected, final boolean givenUp, final long type, final Chunk chunk, final Chunk.Id<?> id, final long accessDate) {
        this.rejected = rejected;
        this.givenUp = givenUp;
        this.type = type;
        this.chunk = chunk;
        this.id = id;
        this.accessDate = accessDate;
    }

    static GetCacheReply newRejected() {
        return new GetCacheReply(true, false, 0, null, null, 0);
    }

    static GetCacheReply newGiveUp() {
        return new GetCacheReply(false, true, 0, null, null, 0);
    }

    static GetCacheReply newNotFound(final TypeRegistry<Chunk.Id<?>> idRegistry, final Chunk.Id<?> id, final long accessDate) {
        return new GetCacheReply(false, false, idRegistry.getId(id), null, id, accessDate);
    }

    GetCacheReply(final TypeRegistry<Chunk> chunkRegistry, final Chunk chunk, final long accessDate) {
        this(false, false, chunkRegistry.getId(chunk), chunk, null, accessDate);
    }

    boolean isRejected() {
        return this.rejected;
    }

    boolean isGivenUp() {
        return this.givenUp;
    }

    boolean isNotFound() {
        return this.chunk == null;
    }

    Chunk getChunk() {
        return this.chunk;
    }

    Chunk.Id<?> getId() {
        return this.id;
    }

    long getAccessDate() {
        return this.accessDate;
    }

    /*
     * 先頭バイトは、あったなら 1、無かったなら 2、諦めてるなら 3、拒否ならそれ以外。
     */

    @Override
    public int byteSize() {
        if (this.rejected) {
            return 1;
        } else if (this.givenUp) {
            return 1;
        } else if (this.chunk == null) {
            return 1 + BytesConversion.byteSize("lol", this.type, this.id, this.accessDate);
        } else {
            return 1 + BytesConversion.byteSize("lol", this.type, this.chunk, this.accessDate);
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.rejected) {
            output.write(0);
            return 1;
        } else if (this.givenUp) {
            output.write(3);
            return 1;
        } else if (this.chunk == null) {
            output.write(2);
            return 1 + BytesConversion.toStream(output, "lol", this.type, this.id, this.accessDate);
        } else {
            output.write(1);
            return 1 + BytesConversion.toStream(output, "lol", this.type, this.chunk, this.accessDate);
        }
    }

    static BytesConvertible.Parser<GetCacheReply> getParser(final TypeRegistry<Chunk> chunkRegistry, final TypeRegistry<Chunk.Id<?>> idRegistry) {
        return new BytesConvertible.Parser<GetCacheReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super GetCacheReply> output) throws MyRuleException,
                    IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                int size = 1;
                if (flag[0] == 1) {
                    final long[] type = new long[1];
                    final List<Chunk> chunk = new ArrayList<>(1);
                    final long[] accessDate = new long[1];

                    size += NumberBytesConversion.fromStream(input, maxByteSize - size, type);
                    final BytesConvertible.Parser<? extends Chunk> parser = chunkRegistry.getParser(type[0]);
                    if (parser == null) {
                        throw new MyRuleException("Not registered chunk type ( " + type[0] + " ).");
                    }
                    size += BytesConversion.fromStream(input, maxByteSize - size, "ol", chunk, parser, accessDate);
                    output.add(new GetCacheReply(chunkRegistry, chunk.get(0), accessDate[0]));
                } else if (flag[0] == 2) {
                    final long[] type = new long[1];
                    final List<Chunk.Id<?>> id = new ArrayList<>(1);
                    final long[] accessDate = new long[1];

                    size += NumberBytesConversion.fromStream(input, maxByteSize - size, type);
                    final BytesConvertible.Parser<? extends Chunk.Id<?>> parser = idRegistry.getParser(type[0]);
                    if (parser == null) {
                        throw new MyRuleException("Not registered id type ( " + type[0] + " ).");
                    }
                    size += BytesConversion.fromStream(input, maxByteSize - size, "ol", id, parser, accessDate);
                    output.add(GetCacheReply.newNotFound(idRegistry, id.get(0), accessDate[0]));
                } else if (flag[0] == 3) {
                    output.add(GetCacheReply.newGiveUp());
                } else {
                    output.add(GetCacheReply.newRejected());
                }
                return size;
            }
        };
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName())).append('[');
        if (this.rejected) {
            buff.append("reject");
        } else if (this.givenUp) {
            buff.append("giveUp");
        } else if (this.chunk == null) {
            buff.append("notFound, ")
                    .append(this.id)
                    .append(", ").append(LoggingFunctions.getSimpleDate(this.accessDate));
        } else {
            buff.append(this.chunk.getId())
                    .append(", ").append(LoggingFunctions.getSimpleDate(this.accessDate));
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.rejected ? 1231 : 1237);
        result = prime * result + (this.givenUp ? 1231 : 1237);
        result = prime * result + ((this.chunk == null) ? 0 : this.chunk.hashCode());
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + (int) (this.accessDate ^ (this.accessDate >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof GetCacheReply)) {
            return false;
        }
        final GetCacheReply other = (GetCacheReply) obj;
        if (this.rejected != other.rejected || this.givenUp != other.givenUp) {
            return false;
        }
        if (this.chunk == null) {
            if (other.chunk != null) {
                return false;
            }
        } else if (!this.chunk.equals(other.chunk)) {
            return false;
        }
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        if (this.accessDate != other.accessDate) {
            return false;
        }
        return true;
    }

}
