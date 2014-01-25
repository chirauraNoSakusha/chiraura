/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ProtocolException;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.container.Pair;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * HTTPリクエスト。
 * @author chirauraNoSakusha
 */
final class HttpRequest {

    private static final class Header {

        private final Http.Method method;
        private final String target;
        private final String version;

        private Header(final Http.Method method, final String target, final String version) {
            if (method == null) {
                throw new IllegalArgumentException("Null method.");
            } else if (target == null) {
                throw new IllegalArgumentException("Null target.");
            } else if (version == null) {
                throw new IllegalArgumentException("Null version.");
            }

            this.method = method;
            this.target = target;
            this.version = version;
        }

        public static Header decode(final String line) throws ProtocolException {
            final String spaceRegexp = "[\t ]";

            final String[] tokens = line.split(spaceRegexp + "+");
            if (tokens.length != 3) {
                throw new ProtocolException("Invalid line ( " + line + " ).");
            }

            final Http.Method method;
            try {
                method = Http.Method.valueOf(tokens[0]);
            } catch (final IllegalArgumentException e) {
                throw new ProtocolException("Invalid method ( " + tokens[0] + " ).");
            }

            return new Header(method, tokens[1], tokens[2]);
        }

    }

    private static final Logger LOG = Logger.getLogger(HttpRequest.class.getName());

    private final Header header;
    private final Map<Http.Field, String> fields;
    private final byte[] content;

    private HttpRequest(final Header header, final Map<Http.Field, String> fields, final byte[] content) {
        if (header == null) {
            throw new IllegalArgumentException("Null header.");
        } else if (fields == null) {
            throw new IllegalArgumentException("Null fields.");
        }

        this.header = header;
        this.fields = fields;
        this.content = content;
    }

    Http.Method getMethod() {
        return this.header.method;
    }

    String getTarget() {
        return this.header.target;
    }

    String getVersion() {
        return this.header.version;
    }

    Map<Http.Field, String> getFields() {
        return this.fields;
    }

    byte[] getContent() {
        return this.content;
    }

    /**
     * HTTP リクエストを受信。
     * @param input 入力
     * @return HTTP リクエスト。
     *         入力が終わりの場合は null
     * @throws MyRuleException 行が長すぎる場合
     * @throws IOException 読み込み異常
     */
    final static HttpRequest fromStream(final InputStreamWrapper input) throws MyRuleException, IOException {
        String line;
        while (true) {
            // 先頭の空行を無視。
            line = input.readLine();
            if (line == null) {
                return null;
            } else if (!line.equals("")) {
                break;
            }
        }

        final Header header = Header.decode(line);
        final Map<Http.Field, String> fields = new EnumMap<>(Http.Field.class);
        while (true) {
            line = input.readLine();

            if (line == null || line.equals("")) {
                break;
            }
            try {
                final Pair<Http.Field, String> field = Http.Field.decode(line);
                fields.put(field.getFirst(), field.getSecond());
            } catch (final IllegalArgumentException e) {
                LOG.log(Level.WARNING, "不明な項目 ( \"{0}\" )を飛ばします。", line);
            }
        }

        final String contentLength = fields.get(Http.Field.CONTENT_LENGTH);
        final byte[] content;
        if (contentLength != null) {
            final int length = Integer.parseInt(contentLength);
            content = new byte[length];
            input.completeRead(content);
        } else {
            content = null;
        }

        return new HttpRequest(header, fields, content);
    }

    private StringBuilder headerToNetworkString(final String separator) {
        final StringBuilder buff = (new StringBuilder(this.header.method.name())).append(" ").append(this.header.target).append(" ")
                .append(this.header.version).append(separator);
        for (final Map.Entry<Http.Field, String> entry : this.fields.entrySet()) {
            buff.append(entry.getKey().toNetworkString()).append(": ").append(entry.getValue()).append(separator);
        }
        return buff;
    }

    String toNetworkString() {
        final StringBuilder buff = headerToNetworkString(Http.SEPARATOR);
        buff.append(Http.SEPARATOR);
        if (this.content != null) {
            buff.append(new String(this.content));
        }
        return buff.toString();
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(headerToNetworkString(System.lineSeparator()));
        if (this.content != null) {
            buff.append("...");
            // buff.append(new String(this.content));
        }
        return buff.append(']').toString();
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
                + "Content-Length: 174"
                + Http.SEPARATOR
                + Http.SEPARATOR
                + "bbs=namazuplus&subject=%82%C4%82%B7%82%C6%83X%83%8C&time=1230144297&FROM=%96%BC%96%B3%82%B5&mail=sage&MESSAGE=%82%C4%82%B7%82%C6&submit=%90V%8BK%83X%83%8C%83b%83h%8D%EC%90%AC";
        try (InputStreamWrapper input = new InputStreamWrapper(new ByteArrayInputStream(sample.getBytes()), Charset.forName("US-ASCII"), Http.SEPARATOR, 1024)) {
            final HttpRequest request = fromStream(input);
            System.out.println("[" + request.toNetworkString() + "]");
        }
    }

}
