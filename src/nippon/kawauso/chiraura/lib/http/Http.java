package nippon.kawauso.chiraura.lib.http;

import java.net.ProtocolException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import nippon.kawauso.chiraura.lib.container.Pair;

/**
 * HTTP 関係。
 * @author chirauraNoSakusha
 */
public final class Http {

    /**
     * 改行。
     */
    public static final String SEPARATOR = "\r\n";

    /**
     * 文字コード。
     */
    public static final Charset HEADER_CHARSET = Charset.forName("US-ASCII");

    /**
     * 標準ポート。
     */
    public static final int DEFAULT_PORT = 80;

    @SuppressWarnings("javadoc")
    public static enum Method {
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
        CONNECT,
        OPTIONS,
        TRACE,
    }

    @SuppressWarnings("javadoc")
    public static enum Field {
        ACCEPT,
        ACCEPT_CHARSET,
        ACCEPT_ENCODING,
        ACCEPT_LANGUAGE,
        AUTHORIZATION,
        COOKIE,
        COOKIE2,
        EXPECT,
        FROM,
        HOST,
        IF_MATCH,
        IF_MODIFIED_SINCE,
        IF_NONE_MATCH,
        IF_RANGE,
        IF_UNMODIFIED_SINCE,
        MAX_FORWARDS,
        PROXY_AUTHORIZATION,
        RANGE,
        REFERER,
        TE,
        ACCEPT_RANGES,
        AGE,
        ALLOW,
        ETAG,
        LOCATION,
        PROXY_AUTHENTICATE,
        RETRY_AFTER,
        SERVER,
        SET_COOKIE2,
        VARY,
        WWW_AUTHENTICATE,
        CACHE_CONTROL,
        CONNECTION,
        DATE,
        PRAGMA,
        TRAILER,
        TRANSFER_ENCODING,
        UPGRADE,
        USER_AGENT,
        WARNING,
        CONTENT_ENCODING,
        CONTENT_LANGUAGE,
        CONTENT_LENGTH,
        CONTENT_LOCATION,
        CONTENT_MD5,
        CONTENT_RANGE,
        CONTENT_TYPE,
        EXPIRES,
        LAST_MODIFIED,

        MIME_VERSION,

        DNT, // Do Not Track の略らしい。

        ;

        private static Field fromNetworkString(final String networkString) {
            return valueOf(networkString.trim().toUpperCase().replaceAll("-", "_"));
        }

        public String toNetworkString() {
            return this.name().replaceAll("_", "-");
        }

        public static Pair<Field, String> decode(final String line) throws ProtocolException {
            final int index = line.indexOf(':');
            if (index < 0) {
                throw new ProtocolException("Invalid field \"" + line + "\".");
            }
            final Field field = fromNetworkString(line.substring(0, index));
            final String value = line.substring(index + 1).trim();
            return new Pair<>(field, value);
        }

    }

    @SuppressWarnings("javadoc")
    public static enum Status {
        /*
         * 使う分だけ定義すれば十分。
         */
        OK(200),
        Partial_Content(206),

        Not_Modified(304),

        Bad_Request(400),
        Forbidden(403),
        Not_Found(404),
        Method_Not_Allowed(405),
        Requested_Range_Not_Satisfiable(416),

        Internal_Server_Error(500),
        Not_Implemented(501),

        ;

        private static Map<Integer, Status> numberToStatus;
        static {
            numberToStatus = new HashMap<>();
            for (final Status status : values()) {
                numberToStatus.put(status.number, status);
            }
        }

        public static Status forNumber(final int number) {
            return numberToStatus.get(number);
        }

        private final int number;

        private Status(final int number) {
            this.number = number;
        }

        public int getNumber() {
            return this.number;
        }

        @Override
        public String toString() {
            return (new StringBuilder(Integer.toString(this.number)))
                    .append(' ')
                    .append(this.name().replaceAll("_", " "))
                    .toString();
        }

    }

    @SuppressWarnings("javadoc")
    public static enum ContentType {
        TEXT_PLAIN("text/plain"),
        TEXT_HTML("text/html"),

        ;

        private final String label;

        private ContentType(final String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }

    }

    /**
     * HTTP 形式の日付にする。
     * @param date 日時
     * @return HTTP 形式の日付
     */
    public static String formatDate(final long date) {
        final DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatter.format(new Date(date));
    }

    /**
     * HTTP 形式の日付から日時を取得する。
     * @param dateString HTTP 形式の日付
     * @return 日時
     */
    public static long decodeDate(final String dateString) {
        final DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        try {
            return formatter.parse(dateString).getTime();
        } catch (final ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("javadoc")
    public static void main(final String[] args) {
        final long date = System.currentTimeMillis();
        final long date2 = decodeDate(formatDate(date));
        System.out.println(date + " " + date2 + " " + (date - date2));
    }

}
