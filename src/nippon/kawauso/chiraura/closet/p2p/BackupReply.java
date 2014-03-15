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
final class BackupReply implements Message {

    private final boolean rejected;
    private final boolean giveUp;

    private final boolean success;

    private final long type;
    private final Chunk chunk;

    private BackupReply(final boolean rejected, final boolean giveUp, final boolean success, final long type, final Chunk chunk) {
        this.rejected = rejected;
        this.giveUp = giveUp;
        this.success = success;
        this.type = type;
        this.chunk = chunk;
    }

    static BackupReply newRejected() {
        return new BackupReply(true, false, false, 0, null);
    }

    static BackupReply newGiveUp() {
        return new BackupReply(false, true, false, 0, null);
    }

    static BackupReply newFailure(final TypeRegistry<Chunk> chunkRegistry, final Chunk chunk) {
        if (chunk == null) {
            throw new IllegalArgumentException("Null chunk.");
        }
        return new BackupReply(false, false, false, chunkRegistry.getId(chunk), chunk);
    }

    BackupReply() {
        this(false, false, true, 0, null);
    }

    boolean isRejected() {
        return this.rejected;
    }

    boolean isGivenUp() {
        return this.giveUp;
    }

    boolean isSuccess() {
        return this.success;
    }

    Chunk getChunk() {
        return this.chunk;
    }

    /*
     * 先頭バイトは、成功なら 1、失敗なら 2、諦めたなら 3、拒否ならそれ以外。
     */

    @Override
    public int byteSize() {
        if (this.rejected || this.giveUp || this.success) {
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
        } else if (this.giveUp) {
            output.write(3);
            return 1;
        } else if (this.success) {
            output.write(1);
            return 1;
        } else {
            output.write(2);
            return 1 + BytesConversion.toStream(output, "lo", this.type, this.chunk);
        }
    }

    static BytesConvertible.Parser<BackupReply> getParser(final TypeRegistry<Chunk> chunkRegistry) {
        return new BytesConvertible.Parser<BackupReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super BackupReply> output) throws MyRuleException,
                    IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                int size = 1;
                if (flag[0] == 1) {
                    output.add(new BackupReply());
                } else if (flag[0] == 2) {
                    final long[] type = new long[1];
                    final List<Chunk> chunk = new ArrayList<>(1);
                    size += NumberBytesConversion.fromStream(input, maxByteSize - size, type);
                    final BytesConvertible.Parser<? extends Chunk> parser = chunkRegistry.getParser(type[0]);
                    if (parser == null) {
                        throw new MyRuleException("Not registered chunk type ( " + type[0] + " ).");
                    }
                    size += parser.fromStream(input, maxByteSize - size, chunk);
                    output.add(new BackupReply(false, false, false, type[0], chunk.get(0)));
                } else if (flag[0] == 3) {
                    output.add(BackupReply.newGiveUp());
                } else {
                    output.add(BackupReply.newRejected());
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
        } else if (this.giveUp) {
            buff.append("giveUp");
        } else if (this.success) {
            buff.append("success");
        } else {
            buff.append("failure, ").append(this.chunk.getId());
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.rejected ? 1231 : 1237);
        result = prime * result + (this.giveUp ? 1231 : 1237);
        result = prime * result + (this.success ? 1231 : 1237);
        result = prime * result + ((this.chunk == null) ? 0 : this.chunk.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof BackupReply)) {
            return false;
        }
        final BackupReply other = (BackupReply) obj;
        if (this.rejected != other.rejected || this.giveUp != other.giveUp || this.success != other.success) {
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
