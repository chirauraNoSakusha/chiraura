/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

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
final class Http {

    static final String SEPARATOR = "\r\n";
    static final Charset HEADER_CHARSET = Charset.forName("US-ASCII");

    static enum Method {
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
        CONNECT,
        OPTIONS,
        TRACE,
    }

    static enum Field {
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

        ;

        private static Field fromNetworkString(final String networkString) {
            return valueOf(networkString.toUpperCase().replaceAll("-", "_"));
        }

        String toNetworkString() {
            return this.name().replaceAll("_", "-");
        }

        static Pair<Field, String> decode(final String line) throws ProtocolException {
            final int index = line.indexOf(':');
            if (index < 0) {
                throw new ProtocolException("Invalid field \"" + line + "\".");
            }
            final Field field = fromNetworkString(line.substring(0, index));
            final String value = line.substring(index + 1).replaceFirst("^[ \t]+", "");
            return new Pair<>(field, value);
        }

    }

    enum Status {
        /*
         * 使う分だけ定義すれば十分。
         */
        OK(200),
        Partial_Content(206),

        Not_Modified(304),

        Bad_Request(400),
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

        static Status forNumber(final int number) {
            return numberToStatus.get(number);
        }

        private final int number;

        Status(final int number) {
            this.number = number;
        }

        int getNumber() {
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

    static String formatDate(final long date) {
        final DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        return formatter.format(new Date(date));
    }

    static long decodeDate(final String dateString) {
        final DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        try {
            return formatter.parse(dateString).getTime();
        } catch (final ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void main(final String[] args) {
        final long date = System.currentTimeMillis();
        final long date2 = decodeDate(formatDate(date));
        System.out.println(date + " " + date2 + " " + (date - date2));
    }

}
