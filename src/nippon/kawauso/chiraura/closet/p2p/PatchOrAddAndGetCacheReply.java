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
final class PatchOrAddAndGetCacheReply implements Message {

    private final boolean rejected;
    private final boolean givenUp;

    private final long type;
    private final Mountain chunk;
    private final long accessDate;

    private PatchOrAddAndGetCacheReply(final boolean rejected, final boolean givenUp, final long type, final Mountain chunk, final long accessDate) {
        this.rejected = rejected;
        this.givenUp = givenUp;
        this.type = type;
        this.chunk = chunk;
        this.accessDate = accessDate;
    }

    static PatchOrAddAndGetCacheReply newRejected() {
        return new PatchOrAddAndGetCacheReply(true, false, 0, null, 0);
    }

    static PatchOrAddAndGetCacheReply newGiveUp() {
        return new PatchOrAddAndGetCacheReply(false, true, 0, null, 0);
    }

    PatchOrAddAndGetCacheReply(final TypeRegistry<Chunk> chunkRegistry, final Mountain chunk, final long accessDate) {
        this(false, false, chunkRegistry.getId(chunk), chunk, accessDate);
    }

    boolean isRejected() {
        return this.rejected;
    }

    boolean isGivenUp() {
        return this.givenUp;
    }

    Mountain getChunk() {
        return this.chunk;
    }

    long getAccessDate() {
        return this.accessDate;
    }

    /*
     * 先頭バイトは、成功なら 1、諦めてるなら 2、拒否ならそれ以外。
     */

    @Override
    public int byteSize() {
        if (this.rejected) {
            return 1;
        } else if (this.givenUp) {
            return 1;
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
            output.write(2);
            return 1;
        } else {
            output.write(1);
            return 1 + BytesConversion.toStream(output, "lol", this.type, this.chunk, this.accessDate);
        }
    }

    static BytesConvertible.Parser<PatchOrAddAndGetCacheReply> getParser(final TypeRegistry<Chunk> chunkRegistry) {
        return new BytesConvertible.Parser<PatchOrAddAndGetCacheReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super PatchOrAddAndGetCacheReply> output)
                    throws MyRuleException, IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                int size = 1;
                if (flag[0] == 1) {
                    final long[] type = new long[1];
                    final List<Mountain> chunk = new ArrayList<>(1);
                    final long[] accessDate = new long[1];

                    size += NumberBytesConversion.fromStream(input, maxByteSize - size, type);
                    final BytesConvertible.Parser<? extends Chunk> parser = chunkRegistry.getParser(type[0]);
                    if (parser == null) {
                        throw new MyRuleException("Not registered chunk type ( " + type[0] + " ).");
                    }
                    size += BytesConversion.fromStream(input, maxByteSize - size, "ol", chunk, parser, accessDate);
                    try {
                        output.add(new PatchOrAddAndGetCacheReply(chunkRegistry, chunk.get(0), accessDate[0]));
                    } catch (final ClassCastException e) {
                        throw new MyRuleException(e);
                    }
                } else if (flag[0] == 2) {
                    output.add(PatchOrAddAndGetCacheReply.newGiveUp());
                } else {
                    output.add(PatchOrAddAndGetCacheReply.newRejected());
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
        } else {
            buff.append(this.chunk.getId()).append(", ").append(LoggingFunctions.getSimpleDate(this.accessDate));
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
        result = prime * result + (int) (this.accessDate ^ (this.accessDate >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof PatchOrAddAndGetCacheReply)) {
            return false;
        }
        final PatchOrAddAndGetCacheReply other = (PatchOrAddAndGetCacheReply) obj;
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
        return this.accessDate == other.accessDate;
    }

}
