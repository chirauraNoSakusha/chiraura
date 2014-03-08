/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.StringFunctions;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.http.Http;
import nippon.kawauso.chiraura.lib.http.InputStreamWrapper;

/**
 * 手抜き 2ch クライアント。
 * @author chirauraNoSakusha
 */
public final class Client {

    // インスタンス化防止。
    private Client() {}

    private static final Logger LOG = Logger.getLogger(Client.class.getName());

    /**
     * スレ。
     * dat ファイルから分かる情報だけ。
     * @author chirauraNoSakusha
     */
    private static interface ThreadDat {
        /**
         * 書き込み。
         * @author chirauraNoSakusha
         */
        public static interface Entry {
            /**
             * 名前を返す。
             * @return 名前
             */
            public String getAuthor();

            /**
             * メールアドレスを返す。
             * @return メールアドレス
             */
            public String getMail();

            /**
             * 日付を返す。
             * @return 日付
             */
            public String getDate();

            /**
             * 本文を返す。
             * @return 本文
             */
            public String getMessage();
        }

        /**
         * タイトルを返す。
         * @return タイトル
         */
        public String getTitle();

        /**
         * 書き込みを返す。
         * @return 書き込み
         */
        public List<Entry> getEntries();
    }

    /**
     * スレ。
     * 周辺情報を含む。
     * @author chirauraNoSakusha
     */
    public static interface BbsThread extends ThreadDat {
        /**
         * 最大書き込み数
         */
        public final int ENTRY_LIMIT = ThreadChunk.ENTRY_LIMIT;

        /**
         * 板名を返す。
         * @return 板名
         */
        public String getBoard();

        /**
         * 名前を返す
         * @return 名前
         */
        public String getName();
    }

    /**
     * 再取得時に前回の続きから始められるスレ。
     * @author chirauraNoSakusha
     */
    private static final class ResumableBbsThread implements BbsThread {

        private final String board;
        private final String name;
        private final ThreadDat dat;

        private final byte[] rawDat;
        private final String lastModified;
        private final String eTag;

        private ResumableBbsThread(final String board, final String name, final ThreadDat dat, final byte[] rawDat, final String lastModified, final String eTag) {
            this.board = board;
            this.name = name;
            this.dat = dat;
            this.rawDat = rawDat;
            this.lastModified = lastModified;
            this.eTag = eTag;
        }

        @Override
        public String getBoard() {
            return this.board;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getTitle() {
            return this.dat.getTitle();
        }

        @Override
        public List<Entry> getEntries() {
            return this.dat.getEntries();
        }
    }

    /**
     * 板。
     * subject.txt から分かる情報だけ。
     * @author chirauraNoSakusha
     */
    private static interface BoardDat {

        /**
         * スレ概要。
         * @author chirauraNoSakusha
         */
        public static interface Entry {
            /**
             * スレ名を返す。
             * @return スレ名
             */
            public String getName();

            /**
             * 題名を返す。
             * @return 題名
             */
            public String getTitle();

            /**
             * 書き込み数を返す。
             * @return 書き込み数
             */
            public String getNumOfComments();
        }

        /**
         * スレ一覧を返す
         * @return スレ一覧
         */
        public List<Entry> getEntries();
    }

    /**
     * 板。
     * 周辺情報を含む。
     * @author chirauraNoSakusha
     */
    public static interface BbsBoard extends BoardDat {
        /**
         * 名前を返す
         * @return 名前
         */
        public String getName();
    }

    /**
     * 再取得時に前回の続きから始められるスレ。
     * @author chirauraNoSakusha
     */
    private static final class ResumableBbsBoard implements BbsBoard {

        private final String name;
        private final BoardDat dat;

        private final String lastModified;
        private final String eTag;

        private ResumableBbsBoard(final String name, final BoardDat dat, final String lastModified, final String eTag) {
            this.name = name;
            this.dat = dat;
            this.lastModified = lastModified;
            this.eTag = eTag;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public List<Entry> getEntries() {
            return this.dat.getEntries();
        }

    }

    private static final Charset CHARSET = Charset.forName("Shift_JIS");

    /**
     * 板を取得する。
     * @param server サーバ
     * @param boardName 板名
     * @return 板
     * @throws MyRuleException 異常
     * @throws IOException 通信異常
     */
    public static BbsBoard getBoard(final InetSocketAddress server, final String boardName) throws MyRuleException, IOException {
        final String request = (new StringBuilder("GET /")).append(boardName).append("/subject.txt HTTP/1.1").append(Http.SEPARATOR)
                .append("Host: ").append(server.getHostString()).append(Http.SEPARATOR)
                .append(Http.SEPARATOR).toString();
        final HttpResponse response = sendAndReceive(server, request);
        if (response.getStatus() == Http.Status.OK) {
            return new ResumableBbsBoard(boardName, decodeBoard(new String(response.getContent(), CHARSET)),
                    response.getFields().get(Http.Field.LAST_MODIFIED), response.getFields().get(Http.Field.ETAG));
        } else {
            return null;
        }
    }

    /**
     * 板を取得する。
     * @param server サーバ
     * @param board 取得済みの分
     * @return 板
     * @throws MyRuleException 異常
     * @throws IOException 通信異常
     */
    public static BbsBoard updateBoard(final InetSocketAddress server, final BbsBoard board) throws MyRuleException, IOException {
        if (!(board instanceof ResumableBbsBoard)) {
            return getBoard(server, board.getName());
        } else {
            final ResumableBbsBoard resumable = (ResumableBbsBoard) board;
            final StringBuilder request = (new StringBuilder("GET /")).append(board.getName()).append("/subject.txt HTTP/1.1").append(Http.SEPARATOR)
                    .append("Host: ").append(server.getHostString()).append(Http.SEPARATOR);
            if (resumable.lastModified != null) {
                request.append("If-Modified-Since: ").append(resumable.lastModified).append(Http.SEPARATOR);
            }
            if (resumable.eTag != null) {
                request.append("If-None-Match: ").append(resumable.eTag).append(Http.SEPARATOR);
            }
            request.append(Http.SEPARATOR);
            final HttpResponse response = sendAndReceive(server, request.toString());
            if (response.getStatus() == Http.Status.Not_Modified) {
                return board;
            } else if (response.getStatus() == Http.Status.OK) {
                return new ResumableBbsBoard(board.getName(), decodeBoard(new String(response.getContent(), CHARSET)),
                        response.getFields().get(Http.Field.LAST_MODIFIED), response.getFields().get(Http.Field.ETAG));
            } else {
                return null;
            }
        }
    }

    private static BoardDat.Entry decodeBoardEntry(final String entryString) {
        // name<>title (numOfComments) の形かどうか。
        final String sepPattern1 = ".dat<>";
        final String sepPattern2 = " (";
        final String sepPattern3 = ")";
        final int sep1 = entryString.indexOf(sepPattern1);
        final int sep3 = entryString.lastIndexOf(sepPattern3);
        final int sep2 = entryString.lastIndexOf(sepPattern2, sep3);
        if (sep1 <= 0 || sep2 <= sep1 || sep3 <= sep2 || sep3 != entryString.length() - sepPattern3.length()) {
            throw new IllegalArgumentException("Invalid board line ( " + entryString + " ).");
        }

        final String name = entryString.substring(0, sep1);
        final String title = entryString.substring(sep1 + sepPattern1.length(), sep2);
        final String numOfComments = entryString.substring(sep2 + sepPattern2.length(), sep3);
        return new BoardDat.Entry() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public String getNumOfComments() {
                return numOfComments;
            }
        };
    }

    /**
     * 板を成形する。
     * @param boardString subject.txt の中身
     * @return 板
     */
    private static BoardDat decodeBoard(final String boardString) {
        final List<BoardDat.Entry> entries = new ArrayList<>();
        for (final String line : boardString.split("[\r\n]")) {
            if (!line.equals("")) {
                entries.add(decodeBoardEntry(line));
            }
        }

        return new BoardDat() {
            @Override
            public List<BoardDat.Entry> getEntries() {
                return entries;
            }
        };
    }

    /**
     * スレを取得する
     * @param server サーバ
     * @param boardName 板名
     * @param threadName スレ名
     * @return スレ
     * @throws MyRuleException 異常
     * @throws IOException 通信異常
     */
    public static BbsThread getThread(final InetSocketAddress server, final String boardName, final String threadName) throws MyRuleException, IOException {
        final String request = (new StringBuilder("GET /")).append(boardName).append("/dat/").append(threadName).append(".dat HTTP/1.1")
                .append(Http.SEPARATOR)
                .append("Host: ").append(server.getHostString()).append(Http.SEPARATOR)
                .append(Http.SEPARATOR).toString();
        final HttpResponse response = sendAndReceive(server, request);
        if (response.getStatus() == Http.Status.OK) {
            return new ResumableBbsThread(boardName, threadName, decodeThread(new String(response.getContent(), CHARSET)), response.getContent(),
                    response.getFields().get(Http.Field.LAST_MODIFIED), response.getFields().get(Http.Field.ETAG));
        } else {
            return null;
        }
    }

    /**
     * スレを取得する。
     * @param server サーバ
     * @param thread 取得済みの分
     * @return スレ
     * @throws IOException 通信異常
     * @throws MyRuleException 異常
     */
    public static BbsThread updateThread(final InetSocketAddress server, final BbsThread thread) throws MyRuleException, IOException {
        if (!(thread instanceof ResumableBbsThread)) {
            // 全取得。
            return getThread(server, thread.getBoard(), thread.getName());
        } else {
            // 差分取得。
            final ResumableBbsThread resumable = (ResumableBbsThread) thread;
            final StringBuilder request = (new StringBuilder("GET /")).append(thread.getBoard()).append("/dat/").append(thread.getName())
                    .append(".dat HTTP/1.1").append(Http.SEPARATOR)
                    .append("Host: ").append(server.getHostString()).append(Http.SEPARATOR)
                    .append("Range: bytes=").append(resumable.rawDat.length - 1).append('-').append(Http.SEPARATOR);
            if (resumable.lastModified != null) {
                request.append("If-Modified-Since: ").append(resumable.lastModified).append(Http.SEPARATOR);
            }
            if (resumable.eTag != null) {
                request.append("If-None-Match: ").append(resumable.eTag).append(Http.SEPARATOR);
            }
            request.append(Http.SEPARATOR);
            final HttpResponse response = sendAndReceive(server, request.toString());

            if (response.getStatus() == Http.Status.Partial_Content) {
                if (resumable.rawDat[resumable.rawDat.length - 1] != response.getContent()[0]) {
                    // あぼーんがあったみたいなので、全取得。
                    return getThread(server, thread.getBoard(), thread.getName());
                } else {
                    final byte[] rawDat = new byte[resumable.rawDat.length + response.getContent().length - 1];
                    System.arraycopy(resumable.rawDat, 0, rawDat, 0, resumable.rawDat.length);
                    System.arraycopy(response.getContent(), 1, rawDat, resumable.rawDat.length, response.getContent().length - 1);
                    return new ResumableBbsThread(thread.getBoard(), thread.getName(), decodeThread(new String(rawDat, CHARSET)), rawDat,
                            response.getFields().get(Http.Field.LAST_MODIFIED), response.getFields().get(Http.Field.ETAG));
                }
            } else if (response.getStatus() == Http.Status.Not_Modified) {
                return thread;
            } else if (response.getStatus() == Http.Status.Requested_Range_Not_Satisfiable) {
                // あぼーんがあったみたいなので、全取得。
                return getThread(server, thread.getBoard(), thread.getName());
            } else if (response.getStatus() == Http.Status.OK) {
                return new ResumableBbsThread(thread.getBoard(), thread.getName(), decodeThread(new String(response.getContent(), CHARSET)),
                        response.getContent(), response.getFields().get(Http.Field.LAST_MODIFIED), response.getFields().get(Http.Field.ETAG));
            } else {
                return null;
            }
        }
    }

    private static ThreadDat.Entry decodeThreadEntry(final String entryString) {
        // author<>mail<>date<> message <> の形かどうか。
        final String sepPattern1 = "<>";
        final String sepPattern2 = "<> ";
        final String sepPattern3 = " <>";
        final int sep1 = entryString.indexOf(sepPattern1);
        final int sep2 = entryString.indexOf(sepPattern1, sep1 + sepPattern1.length());
        final int sep3 = entryString.indexOf(sepPattern2, sep2 + sepPattern1.length());
        final int sep4 = entryString.lastIndexOf(sepPattern3);
        if (sep1 <= 0 || sep2 <= sep1 || sep3 <= sep2 || sep4 <= sep3 || sep4 != entryString.length() - sepPattern3.length()) {
            throw new IllegalArgumentException("Invalid thread line ( " + entryString + " ).");
        }
        final String author = entryString.substring(0, sep1);
        final String mail = entryString.substring(sep1 + sepPattern1.length(), sep2);
        final String date = entryString.substring(sep2 + sepPattern1.length(), sep3);
        final String message = entryString.substring(sep3 + sepPattern2.length(), sep4);
        return new ThreadDat.Entry() {
            @Override
            public String getAuthor() {
                return author;
            }

            @Override
            public String getMail() {
                return mail;
            }

            @Override
            public String getDate() {
                return date;
            }

            @Override
            public String getMessage() {
                return message;
            }
        };
    }

    /**
     * スレを成形する。
     * @param threadString dat ファイルの中身
     * @return スレ
     * @throws ProtocolException
     */
    private static ThreadDat decodeThread(final String threadString) {
        final String[] lines = threadString.split("[\r\n]");
        if (lines.length <= 0) {
            throw new IllegalArgumentException("Lack of lines.");
        }

        // 題名。
        final String sepPattern = "<>";
        final int titleSep = lines[0].lastIndexOf(sepPattern);
        if (titleSep < 0) {
            throw new IllegalArgumentException("Invalid firstEntry ( " + lines[0] + " ).");
        }
        final String title = lines[0].substring(titleSep + sepPattern.length());

        lines[0] = lines[0].substring(0, titleSep + sepPattern.length());

        // 書き込み。
        final List<BbsThread.Entry> entries = new ArrayList<>();
        for (final String line : lines) {
            if (!line.equals("")) {
                entries.add(decodeThreadEntry(line));
            }
        }

        return new ThreadDat() {
            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public List<Entry> getEntries() {
                return entries;
            }
        };
    }

    /**
     * スレを作成する。
     * @param server サーバ
     * @param board 板の名前
     * @param title スレのタイトル
     * @param author 名前
     * @param mail メールアドレス
     * @param message 本文
     * @return 成功したら true
     * @throws MyRuleException 異常
     * @throws IOException 通信異常
     */
    public static boolean addThread(final InetSocketAddress server, final String board, final String title, final String author, final String mail,
            final String message) throws MyRuleException, IOException {
        final StringBuilder content = (new StringBuilder("bbs=")).append(StringFunctions.urlEncode(board, CHARSET))
                .append("&subject=").append(StringFunctions.urlEncode(title, CHARSET))
                .append("&time=").append(System.currentTimeMillis())
                .append("&from=").append(StringFunctions.urlEncode(author, CHARSET))
                .append("&mail=").append(StringFunctions.urlEncode(mail, CHARSET))
                .append("&message=").append(StringFunctions.urlEncode(message, CHARSET))
                .append("&submit=").append(StringFunctions.urlEncode("新規スレッド作成", CHARSET));
        final String request = (new StringBuilder("POST /test/bbs.cgi HTTP/1.1")).append(Http.SEPARATOR)
                .append("Host: ").append(server.getHostString()).append(Http.SEPARATOR)
                .append("Content-Length: ").append(content.length()).append(Http.SEPARATOR)
                .append(Http.SEPARATOR)
                .append(content)
                .toString();
        final HttpResponse response = sendAndReceive(server, request);
        return response.getStatus() == Http.Status.OK && checkResult(new String(response.getContent(), CHARSET));
    }

    private static final Pattern PATTERN1 = Pattern.compile("(?i)<TITLE>.*書きこみました.*</TITLE>");
    private static final Pattern PATTERN2 = Pattern.compile("(?i)<!--.* 2ch_X:true .*-->");

    /**
     * 書き込みが成功したかどうか調べる。
     * @param text
     * @return 成功の場合のみ true
     */
    private static boolean checkResult(final String text) {
        return PATTERN1.matcher(text).find() || PATTERN2.matcher(text).find();
    }

    /**
     * 書き込む。
     * @param server サーバ
     * @param board 板の名前
     * @param thread スレの名前
     * @param author 名前
     * @param mail メールアドレス
     * @param message 本文
     * @return 成功したら true
     * @throws MyRuleException 異常
     * @throws IOException 通信異常
     */
    public static boolean addComment(final InetSocketAddress server, final String board, final String thread, final String author, final String mail,
            final String message) throws MyRuleException, IOException {
        final StringBuilder content = (new StringBuilder("bbs=")).append(StringFunctions.urlEncode(board, CHARSET))
                .append("&key=").append(StringFunctions.urlEncode(thread, CHARSET))
                .append("&time=").append(System.currentTimeMillis())
                .append("&from=").append(StringFunctions.urlEncode(author, CHARSET))
                .append("&mail=").append(StringFunctions.urlEncode(mail, CHARSET))
                .append("&message=").append(StringFunctions.urlEncode(message, CHARSET))
                .append("&submit=").append(StringFunctions.urlEncode("書き込む", CHARSET));
        final String request = (new StringBuilder("POST /test/bbs.cgi HTTP/1.1")).append(Http.SEPARATOR)
                .append("Host: ").append(server.getHostString()).append(Http.SEPARATOR)
                .append("Content-Length: ").append(content.length()).append(Http.SEPARATOR)
                .append(Http.SEPARATOR)
                .append(content)
                .toString();
        final HttpResponse response = sendAndReceive(server, request);
        return response.getStatus() == Http.Status.OK && checkResult(new String(response.getContent(), CHARSET));
    }

    private static final Charset HEADER_CHARSET = Charset.forName("US-ASCII");

    private static HttpResponse sendAndReceive(final InetSocketAddress server, final String request) throws MyRuleException, IOException {
        try (final Socket socket = new Socket()) {
            socket.connect(server);

            try (final OutputStream output = new BufferedOutputStream(socket.getOutputStream());
                    final InputStreamWrapper input = new InputStreamWrapper(socket.getInputStream(), HEADER_CHARSET, Http.SEPARATOR, 8192)) {
                final int headerLength = request.indexOf(Http.SEPARATOR + Http.SEPARATOR);
                if (headerLength + 2 * Http.SEPARATOR.length() < request.length()) {
                    LOG.log(Level.FINEST, "送信: [{0}]",
                            request.substring(0, headerLength).replaceAll(Http.SEPARATOR, System.lineSeparator()) + System.lineSeparator() + "...");
                } else {
                    LOG.log(Level.FINEST, "送信: [{0}]",
                            request.substring(0, headerLength).replaceAll(Http.SEPARATOR, System.lineSeparator()) + System.lineSeparator());
                }
                output.write(request.getBytes(HEADER_CHARSET));
                output.flush();

                final HttpResponse response = HttpResponse.fromStream(input);
                LOG.log(Level.FINEST, "受信: [{0}]", response);
                return response;
            }
        }
    }

    private static byte[] read(final File file) throws FileNotFoundException, IOException {
        try (InputStream input = new FileInputStream(file)) {
            return StreamFunctions.completeRead(input, (int) file.length());
        } catch (final MyRuleException e) {
            // 来ないはず。
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    private static void testDecode() throws FileNotFoundException, IOException {
        final byte[] rawBoard = read(new File("resource" + File.separator + "2chProtocol" + File.separator + "2012-09-19-12-08-44.board"));
        final BoardDat board = decodeBoard(new String(rawBoard, Charset.forName("UTF-8")));
        for (final BoardDat.Entry entry : board.getEntries()) {
            System.out.println("BOARD [ " + entry.getName() + " <> " + entry.getTitle() + " <> " + entry.getNumOfComments() + " ]");
        }
        final byte[] rawThread = read(new File("resource" + File.separator + "2chProtocol" + File.separator + "2012-09-19-12-08-44.thread"));
        final ThreadDat thread = decodeThread(new String(rawThread, Charset.forName("UTF-8")));
        for (final ThreadDat.Entry entry : thread.getEntries()) {
            System.out.println("THREAD [ " + entry.getAuthor() + " <> " + entry.getMail() + " <> " + entry.getDate() + " <> " + entry.getMessage() + " ]");
        }
    }

}
