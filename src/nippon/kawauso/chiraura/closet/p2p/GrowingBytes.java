/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.BytesFunctions;
import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.cell.Utf8Cell;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.storage.Chunk;
import nippon.kawauso.chiraura.storage.SkeletalChunk;

/**
 * @author chirauraNoSakusha
 */
final class GrowingBytes extends SkeletalChunk implements Mountain {

    static final class Id implements Chunk.Id<GrowingBytes> {

        private final String name;
        private final Address address;

        Id(final String name) {
            this.name = name;
            this.address = new Address(HashValue.calculateFromBytes(name.getBytes(Global.INTERNAL_CHARSET)).toBigInteger(), HashValue.SIZE);
        }

        @Override
        public Class<GrowingBytes> getChunkClass() {
            return GrowingBytes.class;
        }

        @Override
        public Address getAddress() {
            return this.address;
        }

        @Override
        public int byteSize() {
            return (new Utf8Cell(this.name)).byteSize();
        }

        @Override
        public int toStream(final OutputStream output) throws IOException {
            return (new Utf8Cell(this.name)).toStream(output);
        }

        static BytesConvertible.Parser<Id> getParser() {
            return new BytesConvertible.Parser<GrowingBytes.Id>() {
                @Override
                public int fromStream(final InputStream input, final int maxByteSize, final List<? super Id> output) throws MyRuleException, IOException {
                    final List<Utf8Cell> name = new ArrayList<>(1);
                    final int size = Utf8Cell.getParser().fromStream(input, maxByteSize, name);
                    output.add(new Id(name.get(0).get()));
                    return size;
                }
            };
        }

        @Override
        public String toString() {
            return (new StringBuilder(GrowingBytes.class.getSimpleName())).append('.').append(this.getClass().getSimpleName())
                    .append("[").append(this.name)
                    .append(']').toString();
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof Id)) {
                return false;
            }
            final Id other = (Id) obj;
            return this.name.equals(other.name);
        }

    }

    static final class Entry implements Mountain.Dust<GrowingBytes>, Comparable<Entry> {

        private final long date;
        private final byte[] bytes;

        Entry(final long date, final byte[] bytes) {
            if (bytes == null) {
                throw new IllegalArgumentException("Null bytes.");
            }
            this.date = date;
            this.bytes = bytes;
        }

        Entry(final byte[] bytes) {
            this(System.currentTimeMillis(), bytes);
        }

        @Override
        public int byteSize() {
            return BytesConversion.byteSize("lab", this.date, this.bytes);
        }

        @Override
        public int toStream(final OutputStream output) throws IOException {
            return BytesConversion.toStream(output, "lab", this.date, this.bytes);
        }

        @Override
        public Class<GrowingBytes> getMountainClass() {
            return GrowingBytes.class;
        }

        static BytesConvertible.Parser<Entry> getParser() {
            return new BytesConvertible.Parser<GrowingBytes.Entry>() {
                @Override
                public int fromStream(final InputStream input, final int maxByteSize, final List<? super Entry> output) throws MyRuleException, IOException {
                    final long[] date = new long[1];
                    final byte[][] bytes = new byte[1][];
                    final int size = BytesConversion.fromStream(input, maxByteSize, "lab", date, bytes);
                    output.add(new Entry(date[0], bytes[0]));
                    return size;
                }
            };
        }

        @Override
        public String toString() {
            return (new StringBuilder(GrowingBytes.class.getSimpleName())).append('.').append(this.getClass().getSimpleName())
                    .append("[").append(LoggingFunctions.getSimpleDate(this.date))
                    .append(", size=").append(this.bytes.length)
                    .append(']').toString();
        }

        @Override
        public int compareTo(final Entry o) {
            if (this.date < o.date) {
                return -1;
            } else if (this.date > o.date) {
                return 1;
            }
            return BytesFunctions.compare(this.bytes, o.bytes);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (this.date ^ (this.date >>> 32));
            result = prime * result + Arrays.hashCode(this.bytes);
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof Entry)) {
                return false;
            }
            final Entry other = (Entry) obj;
            return this.date == other.date && Arrays.equals(this.bytes, other.bytes);
        }

    }

    private final Id id;
    private final long date;
    private final NavigableSet<Entry> entries;

    private int entrySize;
    private boolean notHashed;
    private HashValue hashValue;

    private GrowingBytes(final Id id, final long date, final NavigableSet<Entry> entries, final int entrySize, final boolean notHashed,
            final HashValue hashValue) {
        if (id == null) {
            throw new IllegalArgumentException("Null name.");
        } else if (entries == null) {
            throw new IllegalArgumentException("Null entries.");
        }
        this.id = id;
        this.date = date;
        this.entries = entries;
        this.entrySize = entrySize;
        this.notHashed = notHashed;
        this.hashValue = hashValue;
    }

    private GrowingBytes(final Id id, final long date) {
        this(id, date, new TreeSet<Entry>(), 0, true, null);
    }

    GrowingBytes(final String name, final long date) {
        this(new Id(name), date);
    }

    GrowingBytes(final String name) {
        this(name, System.currentTimeMillis());
    }

    @Override
    public long getDate() {
        if (this.entries.isEmpty()) {
            return this.date;
        } else {
            return this.entries.last().date;
        }
    }

    @Override
    public HashValue getHashValue() {
        if (this.notHashed) {
            this.hashValue = super.getHashValue();
            this.notHashed = false;
        }
        return this.hashValue;
    }

    @Override
    public Id getId() {
        return this.id;
    }

    @Override
    public GrowingBytes copy() {
        return new GrowingBytes(this.id, this.date, new TreeSet<>(this.entries), this.entrySize, this.notHashed, this.hashValue);
    }

    @Override
    public long getFirstDate() {
        return this.date;
    }

    @Override
    public List<Entry> getDiffsAfter(final long baseDate) {
        return new ArrayList<>(this.entries.tailSet(new Entry(baseDate + 1, new byte[0]), true));
    }

    @Override
    public boolean patchable(final Mountain.Dust<?> diff) {
        if (!(diff instanceof Entry)) {
            return false;
        }
        final Entry entry = (Entry) diff;
        return !this.entries.contains(entry);
    }

    @Override
    public boolean patch(final Mountain.Dust<?> diff) {
        if (!(diff instanceof Entry)) {
            throw new IllegalArgumentException("Invalid entry type ( " + diff.getClass().getName() + " ).");
        }
        final Entry entry = (Entry) diff;
        if (this.entries.add(entry)) {
            this.entrySize += entry.byteSize();
            this.notHashed = true;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id.name)
                .append(", ").append(LoggingFunctions.getSimpleDate(this.date))
                .append(", numOfEntries=").append(this.entries.size());
        if (!this.entries.isEmpty()) {
            buff.append(", ").append(LoggingFunctions.getSimpleDate(this.entries.last().date));
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + (int) (this.date ^ (this.date >>> 32));
        result = prime * result + this.entries.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof GrowingBytes)) {
            return false;
        }
        final GrowingBytes other = (GrowingBytes) obj;
        return baseEquals(other) && this.date == other.date && this.entries.equals(other.entries);
    }

    @Override
    public boolean baseEquals(final Mountain o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof GrowingBytes)) {
            return false;
        }
        final GrowingBytes other = (GrowingBytes) o;
        return this.id.equals(other.id);
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("oli", this.id, this.date, this.entries.size()) + this.entrySize;
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        int size = BytesConversion.toStream(output, "oli", this.id, this.date, this.entries.size());
        for (final Entry entry : this.entries) {
            size += entry.toStream(output);
        }
        return size;
    }

    static BytesConvertible.Parser<GrowingBytes> getParser() {
        return new BytesConvertible.Parser<GrowingBytes>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super GrowingBytes> output) throws MyRuleException, IOException {
                final List<Id> id = new ArrayList<>(1);
                final long[] date = new long[1];
                final int[] numOfEntries = new int[1];
                int size = BytesConversion.fromStream(input, maxByteSize, "oli", id, Id.getParser(), date, numOfEntries);
                final GrowingBytes instance = new GrowingBytes(id.get(0), date[0]);
                for (int i = 0; i < numOfEntries[0]; i++) {
                    final List<Entry> entry = new ArrayList<>(1);
                    size += Entry.getParser().fromStream(input, maxByteSize - size, entry);
                    instance.patch(entry.get(0));
                }
                output.add(instance);
                return size;
            }
        };
    }

    public static void main(final String[] args) {
        final GrowingBytes instance = new GrowingBytes("test");
        System.out.println(instance.byteSize() + " " + instance.entrySize);
        for (int i = 0; i < 10; i++) {
            final Entry entry = new Entry(i, new byte[] { (byte) i });
            instance.patch(entry);
            System.out.println(instance.byteSize() + " " + instance.entrySize + " " + entry.byteSize());
        }
        System.out.println(BytesConversion.toBytes(instance).length);
    }

}
