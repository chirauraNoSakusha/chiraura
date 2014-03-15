package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.ArrayFunctions;
import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.storage.Chunk;
import nippon.kawauso.chiraura.storage.Storage;

/**
 * データ片の在庫情報。
 * @author chirauraNoSakusha
 */
final class StockEntry implements BytesConvertible {

    private final long type;
    private final Chunk.Id<?> id;
    private final long date;
    private final HashValue hashValue;

    private StockEntry(final long type, final Chunk.Id<?> id, final long date, final HashValue hashValue) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        } else if (hashValue == null) {
            throw new IllegalArgumentException("Null hash value.");
        }
        this.type = type;
        this.id = id;
        this.date = date;
        this.hashValue = hashValue;
    }

    StockEntry(final TypeRegistry<Chunk.Id<?>> idRegistry, final Chunk.Id<?> id, final long date, final HashValue hashValue) {
        this(idRegistry.getId(id), id, date, hashValue);
    }

    Chunk.Id<?> getId() {
        return this.id;
    }

    long getDate() {
        return this.date;
    }

    HashValue getHashValue() {
        return this.hashValue;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("lolo", this.type, this.id, this.date, this.hashValue);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "lolo", this.type, this.id, this.date, this.hashValue);
    }

    static BytesConvertible.Parser<StockEntry> getParser(final TypeRegistry<Chunk.Id<?>> idRegistry) {
        return new BytesConvertible.Parser<StockEntry>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super StockEntry> output) throws MyRuleException, IOException {
                final long[] type = new long[1];
                final List<Chunk.Id<?>> id = new ArrayList<>(1);
                final long[] date = new long[1];
                final List<HashValue> hashValue = new ArrayList<>(1);

                int size = NumberBytesConversion.fromStream(input, maxByteSize, type);
                final BytesConvertible.Parser<? extends Chunk.Id<?>> parser = idRegistry.getParser(type[0]);
                if (parser == null) {
                    throw new MyRuleException("Not registered chunk id type ( " + type[0] + " ).");
                }
                size += BytesConversion.fromStream(input, maxByteSize - size, "olo", id, parser, date, hashValue, HashValue.getParser());
                output.add(new StockEntry(type[0], id.get(0), date[0], hashValue.get(0)));

                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id)
                .append(", ").append(LoggingFunctions.getSimpleDate(this.date))
                .append(", ").append(this.hashValue)
                .append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + (int) (this.date ^ (this.date >>> 32));
        result = prime * result + this.hashValue.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof StockEntry)) {
            return false;
        }
        final StockEntry other = (StockEntry) obj;
        return this.id.equals(other.id) && this.date == other.date && this.hashValue.equals(other.hashValue);
    }

    /**
     * 保持するデータ片を列挙する。
     * @param storage 倉庫
     * @param start 列挙するデータ片のアドレス範囲の先頭
     * @param end 列挙するデータ片のアドレス範囲の末尾
     * @param limit 列挙する最大数
     * @param exclusive 列挙しないデータ片
     * @param idRegistry エントリ作成のために使う登記簿
     * @return 保持するデータ片のうち、exclusive に含まれていない、または、exclusive に含まれているものより新しいもの。
     *         それらが limit 個を超える場合、前半はより新しいもの、後半は残りから無作為に選ばれたものとなる。
     * @throws InterruptedException 割り込まれた場合
     * @throws IOException 読み込み異常
     */
    static List<StockEntry> getStockedEntries(final StorageWrapper storage, final Address start, final Address end, final int limit,
            final List<StockEntry> exclusive, final TypeRegistry<Chunk.Id<?>> idRegistry) throws IOException, InterruptedException {

        final Storage.Index[] filtered = filter(storage.getIndices(start, end), exclusive);

        if (filtered.length > limit) {
            ArrayFunctions.orderSelect(filtered, limit / 2, 0, filtered.length, new Comparator<Storage.Index>() {
                @Override
                public int compare(final Storage.Index index1, final Storage.Index index2) {
                    if (index1.getDate() > index2.getDate()) {
                        return -1;
                    } else if (index1.getDate() < index2.getDate()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });

            ArrayFunctions.randomSelect(filtered, limit - (limit / 2), (limit / 2) + 1, filtered.length);
        }

        final int n = Math.min(limit, filtered.length);
        final List<StockEntry> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            final Storage.Index index = filtered[i];
            entries.add(new StockEntry(idRegistry, index.getId(), index.getDate(), index.getHashValue()));
        }

        return entries;
    }

    private static Storage.Index[] filter(final Collection<Storage.Index> before, final List<StockEntry> exclusive) {
        final Map<Chunk.Id<?>, StockEntry> blacklist = new HashMap<>();
        for (final StockEntry entry : exclusive) {
            blacklist.put(entry.getId(), entry);
        }

        final List<Storage.Index> after = new ArrayList<>(before.size());
        for (final Storage.Index index : before) {
            final StockEntry black = blacklist.get(index.getId());
            if (black == null) {
                after.add(index);
            } else if (!Mountain.class.isAssignableFrom(index.getId().getChunkClass())) {
                // 差分形式ではない。
                if (black.getDate() < index.getDate()) {
                    // 新しいのを持ってる。
                    after.add(index);
                }
            } else {
                // 差分形式。
                if (black.getDate() < index.getDate()) {
                    // 新しい差分を持ってる。
                    after.add(index);
                } else if (black.getDate() == index.getDate() && !black.getHashValue().equals(index.getHashValue())) {
                    // 同じ日付だけど中身が違うのを持ってる。
                    after.add(index);
                }
            }
        }
        return after.toArray(new Storage.Index[0]);
    }

}
