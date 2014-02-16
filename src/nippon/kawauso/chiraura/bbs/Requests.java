/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ProtocolException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.bbs.Http.Field;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 2chブラウザからのリクエストを分類する。
 * @author chirauraNoSakusha
 */
final class Requests {

    // インスタンス化防止。
    private Requests() {}

    private static final Logger LOG = Logger.getLogger(Requests.class.getName());

    private static final String THREAD_DIR = "dat";
    private static final String THREAD_SUFFIX = ".dat";
    private static final String POST_DIR = "test";
    private static final String POST_FILE = "bbs.cgi";

    static Request fromHttpRequest(final HttpRequest request) throws ProtocolException, IOException {
        if (request.getMethod() == Http.Method.GET) {
            return makeGetRequest(request.getTarget(), request.getFields());
        } else if (request.getMethod() == Http.Method.POST) {
            return makePostRequest(request.getTarget(), request.getContent());
        } else {
            return new NotImplementedRequest(request.getMethod());
        }
    }

    private static Request makeGetRequest(final String target, final Map<Field, String> fields) {
        final String[] tokens = target.split("/");
        if (tokens.length == 0) {
            // / アクセスは不許可。
            return new ForbiddenRequest(target);
        } else if (!tokens[0].equals("")) { // tokens.length == 1 を含む。
            return new NotFoundRequest(target);
        }

        if (tokens.length == 2 && target.charAt(target.length() - 1) == '/') {
            // GET /[board]/ HTTP/1.1
            return new GetIndexRequest(tokens[1]);
        } else if (tokens.length == 3) {
            // 板 (スレタイ一覧)。
            // GET /[board]/subject.txt HTTP/1.1
            if (!tokens[2].equals(Constants.BOARD_FILE)) {
                return new NotFoundRequest(target);
            }
            Long ifModifiedSince = null;
            if (fields.containsKey(Http.Field.IF_MODIFIED_SINCE)) {
                try {
                    ifModifiedSince = Http.decodeDate(fields.get(Http.Field.IF_MODIFIED_SINCE));
                } catch (final IllegalArgumentException e) {
                    LOG.log(Level.WARNING, "非対応の日付 ( " + fields.get(Http.Field.IF_MODIFIED_SINCE) + " ) です。");
                }
            }
            return new GetBoardRequest(tokens[1], ifModifiedSince, fields.get(Http.Field.IF_NONE_MATCH));
        } else if (tokens.length == 4) {
            // スレ。
            // GET /[board]/dat/[thread].dat HTTP/1.1
            if (!tokens[2].equals(THREAD_DIR) || !tokens[3].endsWith(THREAD_SUFFIX)) {
                return new NotFoundRequest(target);
            }
            long thread;
            try {
                thread = Long.parseLong(tokens[3].substring(0, tokens[3].length() - THREAD_SUFFIX.length()));
            } catch (final NumberFormatException e) {
                return new NotFoundRequest(target);
            }
            Long ifModifiedSince = null;
            if (fields.containsKey(Http.Field.IF_MODIFIED_SINCE)) {
                try {
                    ifModifiedSince = Http.decodeDate(fields.get(Http.Field.IF_MODIFIED_SINCE));
                } catch (final IllegalArgumentException e) {
                    LOG.log(Level.WARNING, "非対応の日付 ( " + fields.get(Http.Field.IF_MODIFIED_SINCE) + " ) です。");
                }
            }
            Integer rangeHead = null;
            if (fields.containsKey(Http.Field.RANGE)) {
                final String value = fields.get(Http.Field.RANGE);
                if (!value.matches("bytes= ?[1-9][0-9]*-")) {
                    LOG.log(Level.WARNING, "非対応の範囲指定 ( " + value + " ) です。");
                } else {
                    try {
                        rangeHead = Integer.parseInt(value.substring("bytes=".length(), value.length() - 1));
                    } catch (final IllegalArgumentException e) {
                        LOG.log(Level.WARNING, "非対応の範囲指定 ( " + value + " ) です。");
                    }
                }
            }
            return new GetThreadRequest(tokens[1], thread, ifModifiedSince, fields.get(Http.Field.IF_NONE_MATCH), rangeHead, fields.get(Http.Field.HOST));
        } else {
            return new NotFoundRequest(target);
        }
    }

    private static Request makePostRequest(final String target, final byte[] content) throws ProtocolException {
        final String[] tokens = target.split("/");

        // POST /test/bbs.cgi HTTP/1.1
        if (tokens.length != 3 || !tokens[0].equals("") || !tokens[1].equals(POST_DIR) || !tokens[2].equals(POST_FILE)) {
            return new NotFoundRequest(target);
        } else if (content == null) {
            return new BadHttpRequest("投稿の中身がありません。");
        }

        final Map<Post.Entry, String> entries = Post.Entry.decodeEntries(new String(content, Post.CHARSET));

        if (!entries.containsKey(Post.Entry.SUBMIT)) {
            return new PostErrorRequest("投稿指定がありません。", "投稿指定 ( " + Post.Entry.SUBMIT.name() + " ) がありません。");
        } else if (!entries.containsKey(Post.Entry.BBS)) {
            return new PostErrorRequest("板指定がありません。", "板指定 ( " + Post.Entry.BBS.name() + " ) がありません。");
        } else if (!entries.containsKey(Post.Entry.FROM)) {
            return new PostErrorRequest("名前指定がありません。", "名前指定 ( " + Post.Entry.FROM.name() + " ) がありません。");
        } else if (!entries.containsKey(Post.Entry.MAIL)) {
            return new PostErrorRequest("メールアドレス指定がありません。", "メールアドレス指定 ( " + Post.Entry.MAIL.name() + " ) がありません。");
        } else if (!entries.containsKey(Post.Entry.TIME)) {
            return new PostErrorRequest("日時指定がありません。", "日時指定 ( " + Post.Entry.TIME.name() + " ) がありません。");
        } else if (!entries.containsKey(Post.Entry.MESSAGE)) {
            return new PostErrorRequest("本文指定がありません。", "本文指定 ( " + Post.Entry.MESSAGE.name() + " ) がありません。");
        }

        final String author = entries.get(Post.Entry.FROM);
        final String mail = entries.get(Post.Entry.MAIL);
        final String comment = entries.get(Post.Entry.MESSAGE);

        final long date = Long.parseLong(entries.get(Post.Entry.TIME));
        if ((entries.containsKey(Post.Entry.SUBJECT) && !entries.get(Post.Entry.SUBJECT).isEmpty()) // ギコナビは空の SUBJECT を送ってくるので、それへの対処。
                && (entries.get(Post.Entry.SUBMIT).equals(Post.SUBMIT_ADD_THREAD) || entries.get(Post.Entry.SUBMIT).equals(Post.SUBMIT_ADD_COMMENT))) {
            // スレ作成。
            // bbs=[板名]&subject=[スレのタイトル]&FROM=[名前]&mail=[メール]&MESSAGE=[本文]&submit=新規スレ作成&time=[投稿時間]
            // navi2ch 形式。
            // bbs=[板名]&subject=[スレのタイトル]&FROM=[名前]&mail=[メール]&MESSAGE=[本文]&submit=書き込む&time=[投稿時間]
            return new AddThreadRequest(entries.get(Post.Entry.BBS), entries.get(Post.Entry.SUBJECT), author, mail, date, comment);
        } else if (entries.get(Post.Entry.SUBMIT).equals(Post.SUBMIT_ADD_COMMENT)) {
            // 書き込み。
            // bbs=[板名]&key=[スレ名]&FROM=[名前]&mail=[メール]&MESSAGE=[本文]&submit=書き込む&time=[投稿時間]
            long thread;
            try {
                thread = Long.parseLong(entries.get(Post.Entry.KEY));
            } catch (final NumberFormatException e) {
                return new NotFoundRequest(target);
            }
            return new AddCommentRequest(entries.get(Post.Entry.BBS), thread, author, mail, date, comment);
        } else {
            return new PostErrorRequest("非対応の投稿です。", "非対応の投稿です。");
        }
    }

    public static void main(final String[] args) throws MyRuleException, IOException {
        final String sample = "POST /test/bbs.cgi HTTP/1.0"
                + Http.SEPARATOR
                + "MIME-Version: 1.0"
                + Http.SEPARATOR
                + "Host: localhost:22222"
                + Http.SEPARATOR
                + "Connection: close"
                + Http.SEPARATOR
                + "User-Agent: Monazilla/1.00 Navi2ch"
                + Http.SEPARATOR
                + "Accept-Encoding: gzip"
                + Http.SEPARATOR
                + "Content-Length: 198"
                + Http.SEPARATOR
                + Http.SEPARATOR
                + "bbs=namazuplus&subject=%83e%83X%83g%83e%83X%83g"
                + "%82%C4%82%B7%82%C6%83X%83%8C&time=1230144297&FROM=%96%BC%96%B3%82%B5&mail=sage&MESSAGE=%82%C4%82%B7%82%C6&submit=%90V%8BK%83X%83%8C%83b%83h%8D%EC%90%AC";
        try (InputStreamWrapper input = new InputStreamWrapper(new ByteArrayInputStream(sample.getBytes()), Charset.forName("US-ASCII"), Http.SEPARATOR, 1024)) {
            final HttpRequest httpRequest = HttpRequest.fromStream(input);
            final Request request = fromHttpRequest(httpRequest);
            System.out.println(request);
        }
    }

}
