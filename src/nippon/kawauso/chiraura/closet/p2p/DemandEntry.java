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
import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.storage.Chunk;
import nippon.kawauso.chiraura.storage.Chunk.Id;
import nippon.kawauso.chiraura.storage.Storage;

/**
 * データ片の発注単位。
 * @author chirauraNoSakusha
 */
final class DemandEntry implements BytesConvertible {

    private final long type;
    private final Chunk.Id<?> id;

    private final long date;
    private final HashValue hashValue;

    private DemandEntry(final long type, final Chunk.Id<?> id, final long stockDate, final HashValue stockHashValue) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        }
        this.type = type;
        this.id = id;
        this.date = stockDate;
        this.hashValue = stockHashValue;
    }

    DemandEntry(final TypeRegistry<Chunk.Id<?>> idRegistry, final Chunk.Id<?> id) {
        this(idRegistry.getId(id), id, 0, null);
    }

    DemandEntry(final TypeRegistry<Chunk.Id<?>> idRegistry, final Chunk.Id<?> id, final long date, final HashValue hashValue) {
        this(idRegistry.getId(id), id, date, hashValue);
        if (hashValue == null) {
            throw new IllegalArgumentException("Null stock hash value.");
        }
    }

    boolean isStocked() {
        return this.hashValue != null;
    }

    Chunk.Id<?> getId() {
        return this.id;
    }

    long getStockDate() {
        return this.date;
    }

    HashValue getStockHashValue() {
        return this.hashValue;
    }

    /*
     * 先頭バイトは在庫がある場合は1、ない場合はそれ以外。
     */

    @Override
    public int byteSize() {
        if (this.hashValue == null) {
            return 1 + BytesConversion.byteSize("lo", this.type, this.id);
        } else {
            return 1 + BytesConversion.byteSize("lolo", this.type, this.id, this.date, this.hashValue);
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.hashValue == null) {
            output.write(0);
            return 1 + BytesConversion.toStream(output, "lo", this.type, this.id);
        } else {
            output.write(1);
            return 1 + BytesConversion.toStream(output, "lolo", this.type, this.id, this.date, this.hashValue);
        }
    }

    static BytesConvertible.Parser<DemandEntry> getParser(final TypeRegistry<Chunk.Id<?>> idRegistry) {
        return new BytesConvertible.Parser<DemandEntry>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super DemandEntry> output) throws MyRuleException, IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                int size = 1;

                final long[] type = new long[1];
                size += NumberBytesConversion.fromStream(input, maxByteSize - size, type);
                final BytesConvertible.Parser<? extends Chunk.Id<?>> parser = idRegistry.getParser(type[0]);
                if (parser == null) {
                    throw new MyRuleException("Not registered chunk id type ( " + type[0] + " ).");
                }

                if (flag[0] == 1) {
                    final List<Chunk.Id<?>> id = new ArrayList<>(1);
                    final long[] date = new long[1];
                    final List<HashValue> hashValue = new ArrayList<>(1);

                    size += BytesConversion.fromStream(input, maxByteSize - size, "olo", id, parser, date, hashValue, HashValue.getParser());
                    output.add(new DemandEntry(type[0], id.get(0), date[0], hashValue.get(0)));
                } else {
                    final List<Chunk.Id<?>> id = new ArrayList<>(1);

                    size += parser.fromStream(input, maxByteSize - size, id);
                    output.add(new DemandEntry(type[0], id.get(0), 0, null));
                }
                return size;
            }
        };
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id);
        if (this.hashValue != null) {
            buff.append(", ").append(LoggingFunctions.getSimpleDate(this.date))
                    .append(", ").append(this.hashValue);
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + (int) (this.date ^ (this.date >>> 32));
        result = prime * result + ((this.hashValue == null) ? 0 : this.hashValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof DemandEntry)) {
            return false;
        }
        final DemandEntry other = (DemandEntry) obj;
        if (!this.id.equals(other.id) || this.date != other.date) {
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

    /**
     * 候補の中から保持していないデータ片を列挙する。
     * @param storage 倉庫
     * @param start 全ての候補を含むアドレス範囲の先頭
     * @param end 全ての候補を含むアドレス範囲の末尾
     * @param limit 列挙する最大数
     * @param candidates 候補
     * @param idRegistry エントリ作成のために使う登記簿
     * @return 候補のうち、保持していない、または、保持しているが候補より古いもの。
     *         それらが limit 個を超える場合、てきとうに切り詰める。
     * @throws InterruptedException
     * @throws IOException
     * @throws MyRuleException
     */
    static List<DemandEntry> getDemandedEntries(final StorageWrapper storage, final Address start, final Address end, final int limit,
            final List<StockEntry> candidates, final TypeRegistry<Chunk.Id<?>> idRegistry) throws IOException, InterruptedException {
        final List<DemandEntry> entries = new ArrayList<>();
        for (final StockEntry candidate : candidates) {
            final DemandEntry demand = getDemandedEntry(storage.getIndex(candidate.getId()), candidate, idRegistry);
            if (demand != null) {
                entries.add(demand);
            }
        }
        return entries;
    }

    static DemandEntry getDemandedEntry(final StorageWrapper storage, final StockEntry candidate, final TypeRegistry<Id<?>> idRegistry) throws IOException,
            InterruptedException {
        return getDemandedEntry(storage.getIndex(candidate.getId()), candidate, idRegistry);
    }

    private static DemandEntry getDemandedEntry(final Storage.Index index, final StockEntry candidate, final TypeRegistry<Id<?>> idRegistry) {
        if (index == null) {
            // 持ってない。
            return new DemandEntry(idRegistry, candidate.getId());
        } else if (!Mountain.class.isAssignableFrom(index.getId().getChunkClass())) {
            // 差分形式ではない。
            if (index.getDate() < candidate.getDate()) {
                // 持ってるのが古い。
                return new DemandEntry(idRegistry, index.getId(), index.getDate(), index.getHashValue());
            }
        } else {
            // 差分形式。
            if (index.getDate() < candidate.getDate()) {
                // 持ってるのが古い。
                return new DemandEntry(idRegistry, index.getId(), index.getDate(), index.getHashValue());
            } else if (index.getDate() == candidate.getDate() && !index.getHashValue().equals(candidate.getHashValue())) {
                // 持ってるのが古くはないんだけど、何か中身が違う。
                return new DemandEntry(idRegistry, index.getId(), index.getDate(), index.getHashValue());
            }
        }
        return null;
    }

}
