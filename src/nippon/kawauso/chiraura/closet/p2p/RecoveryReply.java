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
import nippon.kawauso.chiraura.lib.base.HashValue;
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
final class RecoveryReply<T extends Mountain> implements Message {

    private final boolean rejected;

    private final boolean get;

    private final long type;

    private final Chunk chunk;

    private final Chunk.Id<T> id;
    private final List<? extends Mountain.Dust<T>> diffs;
    private final HashValue hashValue;

    private RecoveryReply(final boolean rejected, final boolean get, final long type, final Chunk chunk, final Chunk.Id<T> id,
            final List<? extends Mountain.Dust<T>> diffs, final HashValue hashValue) {
        this.rejected = rejected;
        this.get = get;
        this.type = type;
        this.chunk = chunk;
        this.id = id;
        this.diffs = diffs;
        this.hashValue = hashValue;
    }

    static RecoveryReply<?> newRejected() {
        return new RecoveryReply<>(true, true, 0, null, null, null, null);
    }

    static RecoveryReply<?> newNotFound() {
        return new RecoveryReply<>(false, true, 0, null, null, null, null);
    }

    RecoveryReply(final TypeRegistry<Chunk> chunkRegistry, final Chunk chunk) {
        this(false, true, chunkRegistry.getId(chunk), chunk, null, null, null);
    }

    @SuppressWarnings("unchecked")
    RecoveryReply(final TypeRegistry<Chunk> chunkRegistry, final T chunk, final long date) {
        this(false, false, chunkRegistry.getId(chunk), null, (Chunk.Id<T>) chunk.getId(), (List<? extends Mountain.Dust<T>>) chunk.getDiffsAfter(date),
                chunk.getHashValue());
    }

    boolean isRejected() {
        return this.rejected;
    }

    boolean isNotFound() {
        return this.get && this.chunk == null;
    }

    boolean isGet() {
        return this.get;
    }

    Chunk getChunk() {
        return this.chunk;
    }

    Chunk.Id<T> getId() {
        return this.id;
    }

    List<? extends Mountain.Dust<T>> getDiffs() {
        return this.diffs;
    }

    HashValue getHashValue() {
        return this.hashValue;
    }

    /*
     * 先頭バイトは、データ片なら 1、差分なら 2、無かったら 3、拒否ならそれ以外。
     */

    @Override
    public int byteSize() {
        if (this.rejected) {
            return 1;
        } else if (this.get) {
            if (this.chunk == null) {
                return 1;
            } else {
                return 1 + BytesConversion.byteSize("lo", this.type, this.chunk);
            }
        } else {
            return 1 + BytesConversion.byteSize("loaoo", this.type, this.id, this.diffs, this.hashValue);
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.rejected) {
            output.write(0);
            return 1;
        } else if (this.get) {
            if (this.chunk == null) {
                output.write(3);
                return 1;
            } else {
                output.write(1);
                return 1 + BytesConversion.toStream(output, "lo", this.type, this.chunk);
            }
        } else {
            output.write(2);
            return 1 + BytesConversion.toStream(output, "loaoo", this.type, this.id, this.diffs, this.hashValue);
        }
    }

    static BytesConvertible.Parser<RecoveryReply<?>> getParser(final TypeRegistry<Chunk> chunkRegistry, final TypeRegistry<Chunk.Id<?>> idRegistry,
            final TypeRegistry<Mountain.Dust<?>> diffRegistry) {
        return new BytesConvertible.Parser<RecoveryReply<?>>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super RecoveryReply<?>> output) throws MyRuleException,
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
                    output.add(new RecoveryReply<>(false, true, type[0], chunk.get(0), null, null, null));
                } else if (flag[0] == 2) {
                    size += diff(input, maxByteSize - size, output);
                } else if (flag[0] == 3) {
                    output.add(RecoveryReply.newNotFound());
                } else {
                    output.add(RecoveryReply.newRejected());
                }
                return size;
            }

            private <T extends Mountain> int diff(final InputStream input, final int maxByteSize, final List<? super RecoveryReply<T>> output)
                    throws MyRuleException, IOException {
                final long[] type = new long[1];
                final List<Chunk.Id<T>> id = new ArrayList<>(1);
                final List<? extends Mountain.Dust<T>> diffs = new ArrayList<>();
                final List<HashValue> hashValue = new ArrayList<>(1);

                int size = NumberBytesConversion.fromStream(input, maxByteSize, type);
                final BytesConvertible.Parser<? extends Chunk.Id<?>> idParser = idRegistry.getParser(type[0]);
                final BytesConvertible.Parser<? extends Mountain.Dust<?>> diffParser = diffRegistry.getParser(type[0]);
                if (idParser == null) {
                    throw new MyRuleException("Not registered id type ( " + type[0] + " ).");
                } else if (diffParser == null) {
                    throw new MyRuleException("Not registered diff type ( " + type[0] + " ).");
                }
                size += BytesConversion.fromStream(input, maxByteSize - size, "oaoo", id, idParser, diffs, diffParser, hashValue, HashValue.getParser());
                output.add(new RecoveryReply<>(false, false, type[0], null, id.get(0), diffs, hashValue.get(0)));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName())).append('[');
        if (this.rejected) {
            buff.append("reject");
        } else if (this.get) {
            if (this.chunk == null) {
                buff.append("notFound");
            } else {
                buff.append(this.chunk.getId());
            }
        } else {
            buff.append(this.id)
                    .append(", numOfDiffs").append(this.diffs.size())
                    .append(", ").append(this.hashValue);
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.rejected ? 1231 : 1237);
        result = prime * result + (this.get ? 1231 : 1237);
        result = prime * result + ((this.chunk == null) ? 0 : this.chunk.hashCode());
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.diffs == null) ? 0 : this.diffs.hashCode());
        result = prime * result + ((this.hashValue == null) ? 0 : this.hashValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof RecoveryReply)) {
            return false;
        }
        final RecoveryReply<?> other = (RecoveryReply<?>) obj;
        if (this.rejected != other.rejected || this.get != other.get) {
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
        if (this.diffs == null) {
            if (other.diffs != null) {
                return false;
            }
        } else if (!this.diffs.equals(other.diffs)) {
            return false;
        }
        if (this.hashValue == null) {
            if (other.hashValue != null) {
                return false;
            }
        } else if (!this.hashValue.equals(other.hashValue)) {
            return false;
        }
        return true;
    }

}
