/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.Duration;
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
 * 板 (スレ一覧)。
 * @author chirauraNoSakusha
 */
final class SimpleBoardChunk extends SkeletalChunk implements BoardChunk {

    /**
     * 識別子。
     * @author chirauraNoSakusha
     */
    static final class Id implements Chunk.Id<SimpleBoardChunk> {
        private final String name;
        private final Address address;

        Id(final String name) {
            if (name == null) {
                throw new IllegalArgumentException("Null board name.");
            } else if (ContentConstants.BOARD_LENGTH_LIMIT < name.length()) {
                throw new ContentException("板名が長すぎます。", "最大文字数は " + ContentConstants.BOARD_LENGTH_LIMIT + " です。<BR/>今は " + name.length() + " 文字あります。");
            }
            this.name = name;
            this.address = new Address(HashValue.calculateFromBytes(this.name.getBytes(Global.INTERNAL_CHARSET)).toBigInteger(), HashValue.SIZE);
        }

        @Override
        public Class<SimpleBoardChunk> getChunkClass() {
            return SimpleBoardChunk.class;
        }

        String getName() {
            return this.name;
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
            return new BytesConvertible.Parser<Id>() {
                @Override
                public int fromStream(final InputStream input, final int maxByteSize, final List<? super Id> output) throws MyRuleException, IOException {
                    final List<Utf8Cell> boardName = new ArrayList<>();
                    final int size = Utf8Cell.getParser().fromStream(input, maxByteSize, boardName);
                    try {
                        output.add(new Id(boardName.get(0).get()));
                    } catch (final IllegalArgumentException | ContentException e) {
                        throw new MyRuleException(e);
                    }
                    return size;
                }
            };
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

        @Override
        public String toString() {
            return (new StringBuilder(SimpleBoardChunk.class.getSimpleName())).append('.').append(this.getClass().getSimpleName())
                    .append('[').append(this.name)
                    .append(']').toString();
        }

    }

    /**
     * 構成要素。
     * スレ概要。
     * @author chirauraNoSakusha
     */
    static final class Entry implements BoardChunk.Entry<SimpleBoardChunk> {

        private final long date;
        private final long name;
        private final String title;
        private final int numOfComments;

        Entry(final long date, final long name, final String title, final int numOfComments) {
            if (title == null) {
                throw new IllegalArgumentException("Null thread title.");
            } else if (ContentConstants.TITLE_LENGTH_LIMIT < title.length()) {
                throw new ContentException("タイトルが長すぎます。", "最大文字数は " + ContentConstants.TITLE_LENGTH_LIMIT + " です。<BR/>今は " + title.length() + " 文字あります。");
            } else if (ContentConstants.INVALID_PATTERN.matcher(title).find()) {
                throw new ContentException("タイトルが不正です。", "使用できない文字を含んでいます。");
            } else if (ContentConstants.EMPTY_PATTERN.matcher(title).find()) {
                throw new ContentException("タイトルがありません。");
            }
            this.date = date;
            this.name = name;
            this.title = title;
            this.numOfComments = numOfComments;
        }

        long getName() {
            return this.name;
        }

        String getTitle() {
            return this.title;
        }

        @Override
        public int getNumOfComments() {
            return this.numOfComments;
        }

        @Override
        public long getDate() {
            return this.date;
        }

        @Override
        public long getOrder() {
            return this.date;
        }

        @Override
        public Class<SimpleBoardChunk> getMountainClass() {
            return SimpleBoardChunk.class;
        }

        @Override
        public int byteSize() {
            return BytesConversion.byteSize("lloi", this.date, this.name, new Utf8Cell(this.title), this.numOfComments);
        }

        @Override
        public int toStream(final OutputStream output) throws IOException {
            return BytesConversion.toStream(output, "lloi", this.date, this.name, new Utf8Cell(this.title), this.numOfComments);
        }

        static BytesConvertible.Parser<Entry> getParser() {
            return new BytesConvertible.Parser<Entry>() {
                @Override
                public int fromStream(final InputStream input, final int maxByteSize, final List<? super Entry> output) throws MyRuleException, IOException {
                    final long[] date = new long[1];
                    final long[] thread = new long[1];
                    final List<Utf8Cell> threadTitle = new ArrayList<>(1);
                    final int[] numOfContents = new int[1];
                    final int size = BytesConversion.fromStream(input, maxByteSize, "lloi", date, thread, threadTitle, Utf8Cell.getParser(), numOfContents);
                    try {
                        output.add(new Entry(date[0], thread[0], threadTitle.get(0).get(), numOfContents[0]));
                    } catch (final IllegalArgumentException | ContentException e) {
                        throw new MyRuleException(e);
                    }
                    return size;
                }
            };
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (this.date ^ (this.date >>> 32));
            result = prime * result + (int) (this.name ^ (this.name >>> 32));
            result = prime * result + this.title.hashCode();
            result = prime * result + this.numOfComments;
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
            return this.date == other.date && this.name == other.name && this.title.equals(other.title)
                    && this.numOfComments == other.numOfComments;
        }

        @Override
        public String toString() {
            return (new StringBuilder(SimpleBoardChunk.class.getSimpleName())).append('.').append(this.getClass().getSimpleName())
                    .append('[').append(LoggingFunctions.getSimpleDate(this.date))
                    .append(", ").append(this.name)
                    .append(", ").append(this.title)
                    .append(", ").append(this.numOfComments)
                    .append(']').toString();
        }

    }

    /**
     * スレ概要を日時順に保存するための、異なるスレでは重複しない日時。
     * @author chirauraNoSakusha
     */
    private static final class UniqueDate implements Comparable<UniqueDate> {
        private final long date;
        private final long thread;

        private UniqueDate(final long date, final long thread) {
            this.date = date;
            this.thread = thread;
        }

        @Override
        public int compareTo(final UniqueDate o) {
            if (this.date < o.date) {
                return -1;
            } else if (this.date > o.date) {
                return 1;
            } else if (this.thread < o.thread) {
                return -1;
            } else if (this.thread > o.thread) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (this.date ^ (this.date >>> 32));
            result = prime * result + (int) (this.thread ^ (this.thread >>> 32));
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof UniqueDate)) {
                return false;
            }
            final UniqueDate other = (UniqueDate) obj;
            return this.date == other.date && this.thread == other.thread;
        }
    }

    /*
     * 以下、本体。
     */

    private static final int ENTRY_LIMIT = 2_000;

    private final Id id;
    private final Map<Long, Entry> threadToEntry;
    private final NavigableMap<UniqueDate, Entry> dateToEntry;

    private int entrySize;

    private long updateDate;

    private boolean notHashed;
    private HashValue hashValue;

    private SimpleBoardChunk(final Id id, final Map<Long, Entry> threadToEntry, final NavigableMap<UniqueDate, Entry> dateToEntry, final int entrySize,
            final long updateDate, final boolean notHashed, final HashValue hashValue) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        } else if (threadToEntry == null) {
            throw new IllegalArgumentException("Null thread to entry map.");
        } else if (dateToEntry == null) {
            throw new IllegalArgumentException("Null date to entry map.");
        }
        this.id = id;
        this.threadToEntry = threadToEntry;
        this.dateToEntry = dateToEntry;
        this.entrySize = entrySize;
        this.updateDate = updateDate;
        this.notHashed = notHashed;
        this.hashValue = hashValue;
    }

    private SimpleBoardChunk(final Id id) {
        this(id, new HashMap<Long, Entry>(), new TreeMap<UniqueDate, Entry>(), 0, System.currentTimeMillis(), true, null);
    }

    SimpleBoardChunk(final String boardName) {
        this(new Id(boardName));
    }

    @Override
    public SimpleBoardChunk copy() {
        return new SimpleBoardChunk(this.id, new HashMap<>(this.threadToEntry), new TreeMap<>(this.dateToEntry), this.entrySize, this.updateDate,
                this.notHashed,
                this.hashValue);
    }

    @Override
    public Id getId() {
        return this.id;
    }

    @Override
    public long getFirstDate() {
        if (this.dateToEntry.isEmpty()) {
            return Long.MAX_VALUE;
        } else {
            return this.dateToEntry.firstEntry().getValue().date;
        }
    }

    @Override
    public long getDate() {
        if (this.dateToEntry.isEmpty()) {
            return 0;
        } else {
            return this.dateToEntry.lastEntry().getValue().date;
        }
    }

    @Override
    public long getUpdateDate() {
        return this.updateDate;
    }

    @Override
    public long getNetworkTag() {
        return getHashValue().toBigInteger().longValue();
    }

    @Override
    public List<Entry> getDiffsAfter(final long date) {
        return new ArrayList<>(this.dateToEntry.tailMap(new UniqueDate(date, 0), false).values());
    }

    @Override
    public Entry getEntry(final long thread) {
        return this.threadToEntry.get(thread);
    }

    List<Entry> getEntries() {
        return new ArrayList<>(this.dateToEntry.values());
    }

    @Override
    public boolean patchable(final Mountain.Dust<?> diff) {
        if (!(diff instanceof Entry)) {
            return false;
        }
        final Entry entry = (Entry) diff;
        final Entry old = this.threadToEntry.get(entry.name);
        return old == null || old.date < entry.date || (old.date == entry.date && old.numOfComments < entry.numOfComments);
    }

    @Override
    public boolean patch(final Mountain.Dust<?> diff) {
        if (!(diff instanceof Entry)) {
            throw new IllegalArgumentException("Invalid entry type ( " + diff.getClass().getName() + " ).");
        }
        if (addEntry((Entry) diff)) {
            trim();
            this.updateDate = System.currentTimeMillis();
            return true;
        } else {
            return false;
        }
    }

    private boolean addEntry(final Entry entry) {
        final Entry old = this.threadToEntry.get(entry.name);
        if (old == null) {
            // 新規。
            this.threadToEntry.put(entry.name, entry);
            this.dateToEntry.put(new UniqueDate(entry.date, entry.name), entry);
            this.entrySize += entry.byteSize();
            this.notHashed = true;
            return true;
        } else if (old.date < entry.date || (old.date == entry.date && old.numOfComments < entry.numOfComments)) {
            // 更新。
            this.threadToEntry.put(entry.name, entry);
            this.dateToEntry.remove(new UniqueDate(old.date, old.name));
            this.dateToEntry.put(new UniqueDate(entry.date, entry.name), entry);
            this.entrySize += entry.byteSize() - old.byteSize();
            this.notHashed = true;
            return true;
        } else {
            return false;
        }
    }

    private void trim() {
        while (ContentConstants.BYTE_SIZE_LIMIT < byteSize() || ENTRY_LIMIT < this.threadToEntry.size()) {
            final Entry entry = this.dateToEntry.pollFirstEntry().getValue();
            this.threadToEntry.remove(entry.name);
            this.dateToEntry.remove(new UniqueDate(entry.date, entry.name));
            this.entrySize -= entry.byteSize();
            this.notHashed = true;
        }
    }

    @Override
    public HashValue getHashValue() {
        if (this.notHashed) {
            this.notHashed = false;
            this.hashValue = super.getHashValue();
        }
        return this.hashValue;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("oi", this.id, this.dateToEntry.size()) + this.entrySize;
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        int size = BytesConversion.toStream(output, "oi", this.id, this.dateToEntry.size());
        for (final Entry entry : this.dateToEntry.values()) {
            size += entry.toStream(output);
        }
        return size;
    }

    static BytesConvertible.Parser<SimpleBoardChunk> getParser() {
        return new BytesConvertible.Parser<SimpleBoardChunk>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super SimpleBoardChunk> output) throws MyRuleException,
                    IOException {
                final List<Id> id = new ArrayList<>(1);
                final int[] numOfEntries = new int[1];
                int size = BytesConversion.fromStream(input, maxByteSize, "oi", id, Id.getParser(), numOfEntries);
                final SimpleBoardChunk instance = new SimpleBoardChunk(id.get(0));
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

    @Override
    public String toNetworkString() {
        final StringBuilder buff = new StringBuilder();
        int count = 0;
        for (final Entry entry : this.dateToEntry.descendingMap().values()) {
            if (count >= ContentConstants.BOARD_OUTPUT_LIMIT) {
                break;
            }
            count++;

            buff.append(entry.name)
                    .append(".dat<>").append(entry.title)
                    .append(" (").append(entry.numOfComments)
                    .append(")\n");
        }
        return buff.toString();
    }

    @Override
    public String getContentType() {
        return Http.ContentType.TEXT_PLAIN.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + this.threadToEntry.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof SimpleBoardChunk)) {
            return false;
        }
        final SimpleBoardChunk other = (SimpleBoardChunk) obj;
        return baseEquals(other) && this.entrySize == other.entrySize && this.threadToEntry.equals(other.threadToEntry);
    }

    @Override
    public boolean baseEquals(final Mountain o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof SimpleBoardChunk)) {
            return false;
        }
        final SimpleBoardChunk other = (SimpleBoardChunk) o;
        return this.id.equals(other.id);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id.name)
                .append(", numOfEntries=").append(this.threadToEntry.size())
                .append(']').toString();
    }

    public static void main(final String[] args) throws InterruptedException {
        final String name = "test";
        final SimpleBoardChunk instance = new SimpleBoardChunk(name);

        System.out.println("[" + instance.toNetworkString() + "]");

        Entry entry = new Entry(System.currentTimeMillis(), System.currentTimeMillis() / Duration.SECOND, "ああああうえ", 1);
        System.out.println("add " + entry);
        instance.patch(entry);
        System.out.println("[" + instance.toNetworkString() + "]");

        Thread.sleep(Duration.SECOND + 1L);

        entry = new Entry(System.currentTimeMillis(), System.currentTimeMillis() / Duration.SECOND, "てすと", 1);
        System.out.println("add " + entry);
        instance.patch(entry);
        System.out.println("[" + instance.toNetworkString() + "]");

        System.out.println("[" + instance + "]");
    }

}
