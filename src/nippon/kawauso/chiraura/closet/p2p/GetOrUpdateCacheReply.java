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
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class GetOrUpdateCacheReply<T extends Mountain> implements Message {

    private final boolean rejected;
    private final boolean giveUp;

    private final boolean get;

    private final long type;

    private final T chunk;

    private final Chunk.Id<T> id;
    private final List<? extends Mountain.Dust<T>> diffs;
    private final HashValue hashValue;

    private final long accessDate;

    private GetOrUpdateCacheReply(final boolean rejected, final boolean giveUp, final boolean get, final long type, final T chunk, final Chunk.Id<T> id,
            final List<? extends Mountain.Dust<T>> diffs, final HashValue hashValue, final long accessDate) {
        this.rejected = rejected;
        this.giveUp = giveUp;
        this.get = get;
        this.type = type;
        this.chunk = chunk;
        this.id = id;
        this.diffs = diffs;
        this.hashValue = hashValue;
        this.accessDate = accessDate;
    }

    static GetOrUpdateCacheReply<?> newRejected() {
        return new GetOrUpdateCacheReply<>(true, false, false, 0, null, null, null, null, 0);
    }

    static GetOrUpdateCacheReply<?> newGiveUp() {
        return new GetOrUpdateCacheReply<>(false, true, false, 0, null, null, null, null, 0);
    }

    static <T extends Mountain> GetOrUpdateCacheReply<T> newNotFound(final TypeRegistry<Chunk.Id<?>> idRegistry, final Chunk.Id<T> id, final long accessDate) {
        return new GetOrUpdateCacheReply<>(false, false, true, idRegistry.getId(id), null, id, null, null, accessDate);
    }

    GetOrUpdateCacheReply(final TypeRegistry<Chunk> chunkRegistry, final T chunk, final long accessDate) {
        this(false, false, true, chunkRegistry.getId(chunk), chunk, null, null, null, accessDate);
    }

    @SuppressWarnings("unchecked")
    GetOrUpdateCacheReply(final TypeRegistry<Chunk> chunkRegistry, final T chunk, final long date, final long accessDate) {
        this(false, false, false, chunkRegistry.getId(chunk), null, (Chunk.Id<T>) chunk.getId(), (List<? extends Mountain.Dust<T>>) chunk.getDiffsAfter(date),
                chunk.getHashValue(), accessDate);
    }

    boolean isRejected() {
        return this.rejected;
    }

    boolean isGivenUp() {
        return this.giveUp;
    }

    boolean isNotFound() {
        return this.get && this.chunk == null;
    }

    boolean isGet() {
        return this.get;
    }

    T getChunk() {
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

    long getAccessDate() {
        return this.accessDate;
    }

    /*
     * 先頭バイトは、中身がデータ片なら 1、差分なら 2、そもそもデータ片が無かったら 3、諦めたなら 4、拒否ならそれ以外。
     */

    @Override
    public int byteSize() {
        if (this.rejected) {
            return 1;
        } else if (this.giveUp) {
            return 1;
        } else if (this.get) {
            if (this.chunk == null) {
                return 1 + BytesConversion.byteSize("lol", this.type, this.id, this.accessDate);
            } else {
                return 1 + BytesConversion.byteSize("lol", this.type, this.chunk, this.accessDate);
            }
        } else {
            return 1 + BytesConversion.byteSize("loaool", this.type, this.id, this.diffs, this.hashValue, this.accessDate);
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.rejected) {
            output.write(0);
            return 1;
        } else if (this.giveUp) {
            output.write(4);
            return 1;
        } else if (this.get) {
            if (this.chunk == null) {
                output.write(3);
                return 1 + BytesConversion.toStream(output, "lol", this.type, this.id, this.accessDate);
            } else {
                output.write(1);
                return 1 + BytesConversion.toStream(output, "lol", this.type, this.chunk, this.accessDate);
            }
        } else {
            output.write(2);
            return 1 + BytesConversion.toStream(output, "loaool", this.type, this.id, this.diffs, this.hashValue, this.accessDate);
        }
    }

    static BytesConvertible.Parser<GetOrUpdateCacheReply<?>> getParser(final TypeRegistry<Chunk> chunkRegistry, final TypeRegistry<Chunk.Id<?>> idRegistry,
            final TypeRegistry<Mountain.Dust<?>> diffRegistry) {
        return new BytesConvertible.Parser<GetOrUpdateCacheReply<?>>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super GetOrUpdateCacheReply<?>> output)
                    throws MyRuleException, IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                int size = 1;
                try {
                    if (flag[0] == 1) {
                        size += chunk(input, maxByteSize, output);
                    } else if (flag[0] == 2) {
                        size += diff(input, maxByteSize, output);
                    } else if (flag[0] == 3) {
                        size += notFound(input, maxByteSize, output);
                    } else if (flag[0] == 4) {
                        output.add(GetOrUpdateCacheReply.newGiveUp());
                    } else {
                        output.add(GetOrUpdateCacheReply.newRejected());
                    }
                } catch (final ClassCastException e) {
                    throw new MyRuleException(e);
                }
                return size;
            }

            private <T extends Mountain> int chunk(final InputStream input, final int maxByteSize, final List<? super GetOrUpdateCacheReply<T>> output)
                    throws MyRuleException, IOException {
                final long[] type = new long[1];
                final List<T> chunk = new ArrayList<>(1);
                final long[] accessDate = new long[1];
                int size = NumberBytesConversion.fromStream(input, maxByteSize, type);
                final BytesConvertible.Parser<? extends Chunk> parser = chunkRegistry.getParser(type[0]);
                if (parser == null) {
                    throw new MyRuleException("Not registered chunk type ( " + type[0] + " ).");
                }
                size += BytesConversion.fromStream(input, maxByteSize - size, "ol", chunk, parser, accessDate);
                output.add(new GetOrUpdateCacheReply<>(chunkRegistry, chunk.get(0), accessDate[0]));
                return size;
            }

            private <T extends Mountain> int diff(final InputStream input, final int maxByteSize, final List<? super GetOrUpdateCacheReply<T>> output)
                    throws MyRuleException, IOException {
                final long[] type = new long[1];
                final List<Chunk.Id<T>> id = new ArrayList<>(1);
                final List<? extends Mountain.Dust<T>> diffs = new ArrayList<>();
                final List<HashValue> hashValue = new ArrayList<>(1);
                final long[] accessDate = new long[1];

                int size = NumberBytesConversion.fromStream(input, maxByteSize, type);
                final BytesConvertible.Parser<? extends Chunk.Id<?>> idParser = idRegistry.getParser(type[0]);
                final BytesConvertible.Parser<? extends Mountain.Dust<?>> diffParser = diffRegistry.getParser(type[0]);
                if (idParser == null) {
                    throw new MyRuleException("Not registered id type ( " + type[0] + " ).");
                } else if (diffParser == null) {
                    throw new MyRuleException("Not registered diff type ( " + type[0] + " ).");
                }
                size += BytesConversion.fromStream(input, maxByteSize - size, "oaool", id, idParser, diffs, diffParser, hashValue, HashValue.getParser(),
                        accessDate);
                output.add(new GetOrUpdateCacheReply<>(false, false, false, type[0], null, id.get(0), diffs, hashValue.get(0), accessDate[0]));
                return size;
            }

            private <T extends Mountain> int notFound(final InputStream input, final int maxByteSize, final List<? super GetOrUpdateCacheReply<T>> output)
                    throws MyRuleException, IOException {
                final long[] type = new long[1];
                final List<Chunk.Id<T>> id = new ArrayList<>(1);
                final long[] accessDate = new long[1];
                int size = NumberBytesConversion.fromStream(input, maxByteSize, type);
                final BytesConvertible.Parser<? extends Chunk.Id<?>> parser = idRegistry.getParser(type[0]);
                if (parser == null) {
                    throw new MyRuleException("Not registered id type ( " + type[0] + " ).");
                }
                size += BytesConversion.fromStream(input, maxByteSize - size, "ol", id, parser, accessDate);
                output.add(GetOrUpdateCacheReply.newNotFound(idRegistry, id.get(0), accessDate[0]));
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
        } else if (this.get) {
            if (this.chunk == null) {
                buff.append("notFound, ")
                        .append(LoggingFunctions.getSimpleDate(this.accessDate));
            } else {
                buff.append(this.chunk.getId())
                        .append(", ").append(LoggingFunctions.getSimpleDate(this.accessDate));
            }
        } else {
            if (this.id == null) {
                buff.append("notFound, ")
                        .append(LoggingFunctions.getSimpleDate(this.accessDate));
            } else {
                buff.append(this.id)
                        .append(", numOfDiffs=").append(this.diffs.size())
                        .append(", ")
                        .append(LoggingFunctions.getSimpleDate(this.accessDate));
            }
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.rejected ? 1231 : 1237);
        result = prime * result + (this.giveUp ? 1231 : 1237);
        result = prime * result + (this.get ? 1231 : 1237);
        result = prime * result + ((this.chunk == null) ? 0 : this.chunk.hashCode());
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.diffs == null) ? 0 : this.diffs.hashCode());
        result = prime * result + ((this.hashValue == null) ? 0 : this.hashValue.hashCode());
        result = prime * result + (int) (this.accessDate ^ (this.accessDate >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof GetOrUpdateCacheReply)) {
            return false;
        }
        final GetOrUpdateCacheReply<?> other = (GetOrUpdateCacheReply<?>) obj;
        if (this.rejected != other.rejected || this.giveUp != other.giveUp || this.get != other.get) {
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
        if (this.accessDate != other.accessDate) {
            return false;
        }
        return true;
    }

}
