/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.cell.Utf8Cell;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.storage.Chunk;
import nippon.kawauso.chiraura.storage.SkeletalChunk;

/**
 * スレ。
 * @author chirauraNoSakusha
 */
final class ThreadChunk extends SkeletalChunk implements Mountain, Content {

    /**
     * 識別子。
     * @author chirauraNoSakusha
     */
    static final class Id implements Chunk.Id<ThreadChunk> {
        private final String board;
        private final long thread;
        private final Address address;

        Id(final String board, final long thread) {
            if (board == null) {
                throw new IllegalArgumentException("Null board name.");
            } else if (ContentConstants.BOARD_LENGTH_LIMIT < board.length()) {
                throw new ContentException("板名が長すぎます。", "最大文字数は " + ContentConstants.BOARD_LENGTH_LIMIT + " です。<BR/>今は " + board.length() + " 文字あります。");
            }
            this.board = board;
            this.thread = thread;
            this.address = new Address(HashValue.calculateFromBytes(this.board.getBytes(Global.INTERNAL_CHARSET), new byte[] { 0 },
                    NumberBytesConversion.toBytes(thread)).toBigInteger(), HashValue.SIZE);
        }

        @Override
        public Class<ThreadChunk> getChunkClass() {
            return ThreadChunk.class;
        }

        @Override
        public Address getAddress() {
            return this.address;
        }

        @Override
        public int byteSize() {
            return BytesConversion.byteSize("ol", new Utf8Cell(this.board), this.thread);
        }

        @Override
        public int toStream(final OutputStream output) throws IOException {
            return BytesConversion.toStream(output, "ol", new Utf8Cell(this.board), this.thread);
        }

        static BytesConvertible.Parser<Id> getParser() {
            return new BytesConvertible.Parser<Id>() {
                @Override
                public int fromStream(final InputStream input, final int maxByteSize, final List<? super Id> output) throws MyRuleException, IOException {
                    final List<Utf8Cell> board = new ArrayList<>();
                    final long[] thread = new long[1];
                    final int size = BytesConversion.fromStream(input, maxByteSize, "ol", board, Utf8Cell.getParser(), thread);
                    try {
                        output.add(new Id(board.get(0).get(), thread[0]));
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
            result = prime * result + this.board.hashCode();
            result = prime * result + (int) (this.thread ^ (this.thread >>> 32));
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof Id)) {
                return false;
            }
            final Id other = (Id) obj;
            return this.board.equals(other.board) && this.thread == other.thread;
        }

        @Override
        public String toString() {
            return (new StringBuilder(ThreadChunk.class.getSimpleName())).append('.').append(this.getClass().getSimpleName())
                    .append('[').append(this.board)
                    .append('/').append(Long.toString(this.thread))
                    .append(']').toString();
        }
    }

    /**
     * 構成要素。
     * @author chirauraNoSakusha
     */
    static final class Entry implements Mountain.Dust<ThreadChunk>, Comparable<Entry> {

        private static final int BYTE_SIZE_MAX;
        static {
            int size = 0;
            // 名前の最大バイト数。
            size += ContentConstants.AUTHOR_LENGTH_LIMIT * 6;
            // 名前のバイト数宣言の最大バイト数。
            size += NumberBytesConversion.byteSize(ContentConstants.AUTHOR_LENGTH_LIMIT * 6);
            // メールの最大バイト数。
            size += ContentConstants.MAIL_LENGTH_LIMIT * 6;
            // メールのバイト数宣言の最大バイト数。
            size += NumberBytesConversion.byteSize(ContentConstants.MAIL_LENGTH_LIMIT * 6);
            // 日時の最大バイト数。
            size += NumberBytesConversion.byteSize(Long.MAX_VALUE);
            // ID の最大バイト数。
            size += NumberBytesConversion.byteSize(Long.MAX_VALUE);
            // 書き込み内容の最大バイト数。
            size += ContentConstants.MESSAGE_LENGTH_LIMIT * 6;
            // 書き込み内容のバイト数宣言の最大バイト数。
            size += NumberBytesConversion.byteSize(ContentConstants.MESSAGE_LENGTH_LIMIT * 6);
            BYTE_SIZE_MAX = size;
        }

        private final String author;
        private final String mail;
        private final long date;
        private final long id;
        private final String message;

        private Entry(final String author, final String mail, final long date, final long id, final String message) {
            this.author = author;
            this.mail = mail;
            this.date = date;
            this.id = id;
            this.message = message;
        }

        static Entry newInstance(final String author, final String mail, final long date, final long id, final String message) {
            if (author == null) {
                throw new IllegalArgumentException("Null author.");
            } else if (mail == null) {
                throw new IllegalArgumentException("Null mail.");
            } else if (message == null) {
                throw new IllegalArgumentException("Null message.");
            } else if (ContentConstants.AUTHOR_LENGTH_LIMIT < author.length()) {
                throw new ContentException("名前が長すぎます。", "最大文字数は " + ContentConstants.AUTHOR_LENGTH_LIMIT + " です。<BR/>今は " + author.length() + " 文字あります。");
            } else if (ContentConstants.INVALID_PATTERN.matcher(author).find()) {
                throw new ContentException("名前が不正です。", "使用できない文字を含んでいます。");
            } else if (ContentConstants.MAIL_LENGTH_LIMIT < mail.length()) {
                throw new ContentException("メールアドレスが長すぎます。", "最大文字数は " + ContentConstants.MAIL_LENGTH_LIMIT + " です。<BR/>今は " + mail.length() + " 文字あります。");
            } else if (ContentConstants.INVALID_PATTERN.matcher(mail).find()) {
                throw new ContentException("メールアドレスが不正です。", "使用できない文字を含んでいます。");
            } else if (ContentConstants.MESSAGE_LENGTH_LIMIT < message.length()) {
                throw new ContentException("本文が長すぎます。", "最大文字数は " + ContentConstants.MESSAGE_LENGTH_LIMIT + " です。<BR/>今は " + message.length() + " 文字あります。");
            }
            final String body = message.replace(ContentConstants.SEPARATOR_SYMBOL, "");
            if (ContentConstants.INVALID_PATTERN.matcher(body).find()) {
                throw new ContentException("本文が不正です。", "使用できない文字を含んでいます。");
            } else if (ContentConstants.EMPTY_PATTERN.matcher(body).find()) {
                throw new ContentException("本文がありません。");
            }
            return new Entry(author, mail, date, id, message);
        }

        private static Entry newDummyForComparison(final long date) {
            return new Entry("", "", date, 0, "");
        }

        @Override
        public Class<ThreadChunk> getMountainClass() {
            return ThreadChunk.class;
        }

        @Override
        public int byteSize() {
            return BytesConversion.byteSize("oollo", new Utf8Cell(this.author), new Utf8Cell(this.mail), this.date, this.id, new Utf8Cell(this.message));
        }

        @Override
        public int toStream(final OutputStream output) throws IOException {
            return BytesConversion
                    .toStream(output, "oollo", new Utf8Cell(this.author), new Utf8Cell(this.mail), this.date, this.id, new Utf8Cell(this.message));
        }

        static BytesConvertible.Parser<Entry> getParser() {
            return new BytesConvertible.Parser<Entry>() {
                @Override
                public int fromStream(final InputStream input, final int maxByteSize, final List<? super Entry> output) throws MyRuleException, IOException {
                    final List<Utf8Cell> author = new ArrayList<>(1);
                    final List<Utf8Cell> mail = new ArrayList<>(1);
                    final long[] date = new long[1];
                    final long[] id = new long[1];
                    final List<Utf8Cell> message = new ArrayList<>(1);
                    final int size = BytesConversion.fromStream(input, maxByteSize, "oollo", author, Utf8Cell.getParser(), mail, Utf8Cell.getParser(), date,
                            id, message, Utf8Cell.getParser());
                    try {
                        output.add(Entry.newInstance(author.get(0).get(), mail.get(0).get(), date[0], id[0], message.get(0).get()));
                    } catch (final IllegalArgumentException | ContentException e) {
                        throw new MyRuleException(e);
                    }
                    return size;
                }
            };
        }

        @Override
        public int compareTo(final Entry o) {
            if (this.date < o.date) {
                return -1;
            } else if (this.date > o.date) {
                return 1;
            } else if (this.id < o.id) {
                return -1;
            } else if (this.id > o.id) {
                return 1;
            }
            int result = this.author.compareTo(o.author);
            if (result != 0) {
                return result;
            }
            result = this.mail.compareTo(o.mail);
            if (result != 0) {
                return result;
            }
            return this.message.compareTo(o.message);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.author.hashCode();
            result = prime * result + this.mail.hashCode();
            result = prime * result + (int) (this.date ^ (this.date >>> 32));
            result = prime * result + (int) (this.id ^ (this.id >>> 32));
            result = prime * result + this.message.hashCode();
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
            return this.author.equals(other.author) && this.mail.equals(other.mail) && this.date == other.date && this.id == other.id
                    && this.message.equals(other.message);
        }

        @Override
        public String toString() {
            return (new StringBuilder(ThreadChunk.class.getSimpleName())).append('.').append(this.getClass().getSimpleName())
                    .append('[').append(this.author)
                    .append(", ").append(this.mail)
                    .append(", ").append(LoggingFunctions.getSimpleDate(this.date))
                    .append(", ").append(PostFunctions.idToString(this.id))
                    .append(", messageLength=").append(this.message.length())
                    .append(']').toString();
        }

    }

    /*
     * 以下、本体。
     */

    static final int ENTRY_LIMIT = 1_000;

    private final Id id;
    private final String title;
    private final Entry firstEntry;
    private final NavigableSet<Entry> entries;

    private int entrySize;

    private long updateDate;

    private boolean notHashed;
    private HashValue hashValue;

    // chiraura:// 記法用。
    private String host;

    private ThreadChunk(final Id id, final String title, final Entry firstEntry, final NavigableSet<Entry> entries, final int entrySize, final long updateDate,
            final boolean notHashed, final HashValue hashValue) {
        if (id == null) {
            throw new IllegalArgumentException("Null id.");
        } else if (title == null) {
            throw new IllegalArgumentException("Null title.");
        } else if (firstEntry == null) {
            throw new IllegalArgumentException("Null first entry.");
        } else if (entries == null) {
            throw new IllegalArgumentException("Null entries.");
        } else if (ContentConstants.TITLE_LENGTH_LIMIT < title.length()) {
            throw new ContentException("タイトルが長すぎます。", "最大文字数は " + ContentConstants.TITLE_LENGTH_LIMIT + " です。<BR/>今は " + title.length() + " 文字あります。");
        } else if (ContentConstants.INVALID_PATTERN.matcher(title).find()) {
            throw new ContentException("タイトルが不正です。", "使用できない文字を含んでいます。");
        } else if (ContentConstants.EMPTY_PATTERN.matcher(title).find()) {
            throw new ContentException("タイトルがありません。");
        }
        this.id = id;
        this.title = title;
        this.firstEntry = firstEntry;
        this.entries = entries;
        this.entrySize = entrySize;
        this.updateDate = updateDate;
        this.notHashed = notHashed;
        this.hashValue = hashValue;

        this.host = null;
    }

    private ThreadChunk(final Id id, final String title, final Entry firstEntry) {
        this(id, title, firstEntry, new TreeSet<Entry>(), 0, System.currentTimeMillis(), true, null);
    }

    ThreadChunk(final String board, final long thread, final String title, final String author, final String mail, final long date, final long id,
            final String message) {
        this(new Id(board, thread), title, Entry.newInstance(author, mail, date, id, message));
    }

    @Override
    public ThreadChunk copy() {
        return new ThreadChunk(this.id, this.title, this.firstEntry, new TreeSet<>(this.entries), this.entrySize, this.updateDate, this.notHashed,
                this.hashValue);
    }

    boolean isFull() {
        /*
         * + 1 は entries のバイト数宣言が増えるかもしれないことへの対処。
         */
        return ENTRY_LIMIT - 1 <= this.entries.size() || ContentConstants.BYTE_SIZE_LIMIT < byteSize() + 1 + Entry.BYTE_SIZE_MAX;
    }

    String getBoard() {
        return this.id.board;
    }

    long getName() {
        return this.id.thread;
    }

    String getTitle() {
        return this.title;
    }

    int getNumOfComments() {
        return 1 + this.entries.size();
    }

    @Override
    public Id getId() {
        return this.id;
    }

    @Override
    public long getFirstDate() {
        if (!this.entries.isEmpty()) {
            final long date = this.entries.first().date;
            if (date < this.firstEntry.date) {
                // 好ましい状態ではないが、認める。
                return date;
            }
        }
        return this.firstEntry.date;
    }

    @Override
    public long getDate() {
        if (this.entries.isEmpty()) {
            return this.firstEntry.date;
        } else {
            return this.entries.last().date;
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
        return new ArrayList<>(this.entries.tailSet(Entry.newDummyForComparison(date), false));
    }

    List<Entry> getEntries() {
        return new ArrayList<>(this.entries);
    }

    @Override
    public boolean patchable(final Mountain.Dust<?> diff) {
        if (!(diff instanceof Entry)) {
            return false;
        }
        final Entry entry = (Entry) diff;
        return !isFull() && !this.entries.contains(entry);
    }

    @Override
    public boolean patch(final Mountain.Dust<?> diff) {
        if (!(diff instanceof Entry)) {
            throw new IllegalArgumentException("Invalid entry type ( " + diff.getClass().getName() + " ).");
        }
        if (isFull()) {
            return false;
        }
        final Entry entry = (Entry) diff;
        if (this.entries.add(entry)) {
            this.entrySize += entry.byteSize();
            this.updateDate = System.currentTimeMillis();
            this.notHashed = true;
            return true;
        } else {
            return false;
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
        return BytesConversion.byteSize("oooi", this.id, new Utf8Cell(this.title), this.firstEntry, this.entries.size()) + this.entrySize;
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        int size = BytesConversion.toStream(output, "oooi", this.id, new Utf8Cell(this.title), this.firstEntry, this.entries.size());
        for (final Entry entry : this.entries) {
            size += entry.toStream(output);
        }
        return size;
    }

    static BytesConvertible.Parser<ThreadChunk> getParser() {
        return new BytesConvertible.Parser<ThreadChunk>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super ThreadChunk> output) throws MyRuleException,
                    IOException {
                final List<Id> id = new ArrayList<>(1);
                final List<Utf8Cell> title = new ArrayList<>(1);
                final List<Entry> firstEntry = new ArrayList<>(1);
                final int[] numOfEntries = new int[1];
                int size = BytesConversion.fromStream(input, maxByteSize, "oooi", id, Id.getParser(), title, Utf8Cell.getParser(), firstEntry,
                        Entry.getParser(), numOfEntries);
                final ThreadChunk instance = new ThreadChunk(id.get(0), title.get(0).get(), firstEntry.get(0));
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

    void setHost(final String host, final int port) {
        if (host != null && !host.isEmpty()) {
            final StringBuilder buff = new StringBuilder("(chiraura) ttp://").append(host);
            if (host.indexOf(':') < 0 && port != Http.DEFAULT_PORT) {
                buff.append(':').append(Integer.toString(port));
            }
            this.host = buff.append('/').toString();
        }
    }

    private String wrapMessage(final String msg) {
        if (this.host == null) {
            return msg;
        } else {
            return ContentConstants.CHIRAURA_NOTATION_LABEL.matcher(msg).replaceAll(this.host);
        }
    }

    private static final String TERMINAL = "1001<><>おわり<> もう綴れません <>\n";

    @Override
    public String toNetworkString() {
        final DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd(E) HH:mm:ss");
        final StringBuilder buff = (new StringBuilder(this.firstEntry.author))
                .append("<>").append(this.firstEntry.mail)
                .append("<>").append(formatter.format(new Date(this.firstEntry.date)))
                .append(" ID:").append(PostFunctions.idToString(this.firstEntry.id))
                .append("<> ").append(wrapMessage(this.firstEntry.message))
                .append(" <>").append(this.title)
                .append('\n');
        for (final Entry entry : this.entries) {
            buff.append(entry.author)
                    .append("<>").append(entry.mail)
                    .append("<>").append(formatter.format(new Date(entry.date)))
                    .append(" ID:").append(PostFunctions.idToString(entry.id))
                    .append("<> ").append(wrapMessage(entry.message))
                    .append(" <>\n");
        }
        if (isFull()) {
            buff.append(TERMINAL);
        }
        return buff.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.id.hashCode();
        result = prime * result + this.title.hashCode();
        result = prime * result + this.firstEntry.hashCode();
        result = prime * result + this.entries.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof ThreadChunk)) {
            return false;
        }
        final ThreadChunk other = (ThreadChunk) obj;
        return baseEquals(other) && this.entrySize == other.entrySize && this.entries.equals(other.entries);
    }

    @Override
    public boolean baseEquals(final Mountain o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ThreadChunk)) {
            return false;
        }
        final ThreadChunk other = (ThreadChunk) o;
        // TODO firstEntry は考慮しない方が良いか？
        return this.id.equals(other.id) && this.title.equals(other.title) && this.firstEntry.equals(other.firstEntry);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.id.board)
                .append('/').append(Long.toString(this.id.thread))
                .append(", ").append(this.firstEntry)
                .append(", numOfEntries=").append(Integer.toString(this.entries.size()))
                .append(']').toString();
    }

    public static void main(final String[] args) {
        System.out.println(ContentConstants.BYTE_SIZE_LIMIT + " " + Entry.BYTE_SIZE_MAX + " " + (ContentConstants.BYTE_SIZE_LIMIT - Entry.BYTE_SIZE_MAX) + " "
                + (Entry.BYTE_SIZE_MAX / (double) ContentConstants.BYTE_SIZE_LIMIT));

        final ThreadChunk instance = new ThreadChunk("test", System.currentTimeMillis() / Duration.SECOND, "てすとスレ", "名無し", "age", System.currentTimeMillis(),
                1234567890L, "テストだお。");
        System.out.println("[" + instance.toNetworkString() + "]");
        instance.patch(Entry.newInstance("メシア", "sage", System.currentTimeMillis(), 987654321L, "くそスレ乙"));
        System.out.println("[" + instance.toNetworkString() + "]");
    }

}
