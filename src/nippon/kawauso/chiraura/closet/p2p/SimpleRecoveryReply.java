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
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class SimpleRecoveryReply implements Message {

    private final boolean rejected;

    private final long type;
    private final Chunk chunk;

    private SimpleRecoveryReply(final boolean rejected, final long type, final Chunk chunk) {
        this.rejected = rejected;
        this.type = type;
        this.chunk = chunk;
    }

    static SimpleRecoveryReply newRejected() {
        return new SimpleRecoveryReply(true, 0, null);
    }

    static SimpleRecoveryReply newNotFound() {
        return new SimpleRecoveryReply(false, 0, null);
    }

    SimpleRecoveryReply(final TypeRegistry<Chunk> chunkRegistry, final Chunk chunk) {
        this(false, chunkRegistry.getId(chunk), chunk);
        if (chunk == null) {
            throw new IllegalArgumentException("Null chunk.");
        }
    }

    boolean isRejected() {
        return this.rejected;
    }

    boolean isNotFound() {
        return this.chunk == null;
    }

    Chunk getChunk() {
        return this.chunk;
    }

    /*
     * 先頭バイトは、あったなら 1、無かったなら 2、拒否ならそれ以外。
     */

    @Override
    public int byteSize() {
        if (this.rejected) {
            return 1;
        } else if (this.chunk == null) {
            return 1;
        } else {
            return 1 + BytesConversion.byteSize("lo", this.type, this.chunk);
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.rejected) {
            output.write(0);
            return 1;
        } else if (this.chunk == null) {
            output.write(2);
            return 1;
        } else {
            output.write(1);
            return 1 + BytesConversion.toStream(output, "lo", this.type, this.chunk);
        }
    }

    static BytesConvertible.Parser<SimpleRecoveryReply> getParser(final TypeRegistry<Chunk> chunkRegistry) {
        return new BytesConvertible.Parser<SimpleRecoveryReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super SimpleRecoveryReply> output) throws MyRuleException,
                    IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                int size = 1;
                if (flag[0] == 1) {
                    final long[] type = new long[1];
                    final List<Chunk> chunk = new ArrayList<>(1);

                    size += NumberBytesConversion.fromStream(input, maxByteSize - size, type);
                    final BytesConvertible.Parser<? extends Chunk> parser = chunkRegistry.getParser(type[0]);
                    if (parser == null) {
                        throw new MyRuleException("Not registered chunk type ( " + type[0] + " ).");
                    }
                    size += parser.fromStream(input, maxByteSize - size, chunk);
                    output.add(new SimpleRecoveryReply(chunkRegistry, chunk.get(0)));
                } else if (flag[0] == 2) {
                    output.add(SimpleRecoveryReply.newNotFound());
                } else {
                    output.add(SimpleRecoveryReply.newRejected());
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
        } else if (this.chunk == null) {
            buff.append("notFound");
        } else {
            buff.append(this.chunk.getId());
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.rejected ? 1231 : 1237);
        result = prime * result + ((this.chunk == null) ? 0 : this.chunk.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof SimpleRecoveryReply)) {
            return false;
        }
        final SimpleRecoveryReply other = (SimpleRecoveryReply) obj;
        if (this.rejected != other.rejected) {
            return false;
        }
        if (this.chunk == null) {
            if (other.chunk != null) {
                return false;
            }
        } else if (!this.chunk.equals(other.chunk)) {
            return false;
        }
        return true;
    }

}
