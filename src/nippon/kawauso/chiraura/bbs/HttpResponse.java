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
import java.util.regex.Pattern;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.container.Pair;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.http.Http;
import nippon.kawauso.chiraura.lib.http.InputStreamWrapper;

/**
 * @author chirauraNoSakusha
 */
final class HttpResponse {

    private static final class Header {

        private final Http.Status status;
        private final String version;

        private Header(final Http.Status status, final String version) {
            if (status == null) {
                throw new IllegalArgumentException("Null status.");
            } else if (version == null) {
                throw new IllegalArgumentException("Null version.");
            }

            this.status = status;
            this.version = version;
        }

        private static final Pattern sep = Pattern.compile("[\t ]+");

        public static Header decode(final String line) throws ProtocolException {
            final String[] tokens = sep.split(line);
            if (tokens.length < 2) {
                throw new ProtocolException("Invalid line (" + line + " ).");
            }

            final Http.Status status;
            try {
                status = Http.Status.forNumber(Integer.parseInt(tokens[1]));
            } catch (final IllegalArgumentException e) {
                throw new ProtocolException("Invalid status ( " + tokens[1] + " ).");
            }

            return new Header(status, tokens[0]);
        }

    }

    private static final Logger LOG = Logger.getLogger(HttpRequest.class.getName());

    private final Header header;
    private final Map<Http.Field, String> fields;
    private final byte[] content;

    private HttpResponse(final Header header, final Map<Http.Field, String> fields, final byte[] content) {
        if (header == null) {
            throw new IllegalArgumentException("Null header.");
        } else if (fields == null) {
            throw new IllegalArgumentException("Null fields.");
        }

        this.header = header;
        this.fields = fields;
        this.content = content;
    }

    Http.Status getStatus() {
        return this.header.status;
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
     * HTTP の返答を読み込む。
     * @param input 読み込み元
     * @return HTTP の返答。終端に達していたら null
     * @throws MyRuleException 規約違反
     * @throws IOException 読み込み異常
     */
    final static HttpResponse fromStream(final InputStreamWrapper input) throws MyRuleException, IOException {
        String line;
        while (true) {
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
            content = StreamFunctions.completeRead(input, length);
        } else {
            content = null;
        }

        return new HttpResponse(header, fields, content);
    }

    private StringBuilder headerToString(final String separator) {
        final StringBuilder buff = (new StringBuilder(this.header.version)).append(" ").append(this.header.status).append(separator);
        for (final Map.Entry<Http.Field, String> entry : this.fields.entrySet()) {
            buff.append(entry.getKey().toNetworkString()).append(": ").append(entry.getValue()).append(separator);
        }
        return buff;
    }

    String toNetworkString() {
        final StringBuilder buff = headerToString(Http.SEPARATOR).append(Http.SEPARATOR);
        if (this.content != null) {
            buff.append(new String(this.content));
        }
        return buff.toString();
    }

    @Override
    public String toString() {
        final StringBuilder buff = headerToString(System.lineSeparator());
        if (this.content != null) {
            buff.append("...");
        }
        return buff.toString();
    }

    public static void main(final String[] args) throws MyRuleException, IOException {
        final String sample = "HTTP/1.1 200 OK"
                + Http.SEPARATOR
                + "Content-Length: 174"
                + Http.SEPARATOR
                + Http.SEPARATOR
                + "bbs=namazuplus&subject=%82%C4%82%B7%82%C6%83X%83%8C&time=1230144297&FROM=%96%BC%96%B3%82%B5&mail=sage&MESSAGE=%82%C4%82%B7%82%C6&submit=%90V%8BK%83X%83%8C%83b%83h%8D%EC%90%AC";
        try (InputStreamWrapper input = new InputStreamWrapper(new ByteArrayInputStream(sample.getBytes()), Charset.forName("US-ASCII"), Http.SEPARATOR, 1024)) {
            final HttpResponse response = fromStream(input);
            System.out.println("[" + response.toNetworkString() + "]");
        }
    }

}
