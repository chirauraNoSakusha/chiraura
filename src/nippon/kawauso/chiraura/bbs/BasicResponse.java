/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Map;

import nippon.kawauso.chiraura.lib.http.Http;

/**
 * HTTP レスポンス。
 * フィールドのうち、SERVER, DATE, CONTENT_LENGTH は必要に応じて勝手につくる。
 * @author chirauraNoSakusha
 */
abstract class BasicResponse implements Response {

    private final Http.Status status;
    private final Map<Http.Field, String> fields;
    private final byte[] content;

    BasicResponse(final Http.Status status, final Map<Http.Field, String> fields, final byte[] content) {
        if (status == null) {
            throw new IllegalArgumentException("Null status.");
        }

        this.status = status;
        if (fields != null) {
            this.fields = new EnumMap<>(fields);
        } else {
            this.fields = new EnumMap<>(Http.Field.class);
        }
        this.content = content;

        if (!this.fields.containsKey(Http.Field.SERVER)) {
            this.fields.put(Http.Field.SERVER, Constants.SERVER_LABEL);
        }
        if (!this.fields.containsKey(Http.Field.DATE)) {
            this.fields.put(Http.Field.DATE, Http.formatDate(System.currentTimeMillis()));
        }
        if (this.content != null && !this.fields.containsKey(Http.Field.CONTENT_LENGTH)) {
            this.fields.put(Http.Field.CONTENT_LENGTH, Integer.toString(this.content.length));
        }
    }

    private StringBuilder headerToNetworkString(final String separator) {
        final StringBuilder buff = (new StringBuilder("HTTP/1.1 ")).append(this.status).append(separator);
        for (final Map.Entry<Http.Field, String> entry : this.fields.entrySet()) {
            buff.append(entry.getKey().toNetworkString()).append(": ").append(entry.getValue()).append(separator);
        }
        return buff;
    }

    @Override
    public void toStream(final OutputStream output) throws IOException {
        output.write(headerToNetworkString(Http.SEPARATOR).append(Http.SEPARATOR).toString().getBytes(Http.HEADER_CHARSET));
        if (this.content != null) {
            output.write(this.content);
        }
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(headerToNetworkString(System.lineSeparator()));
        if (this.content != null) {
            buff.append("...");
            // buff.append(new String(this.content, Constants.CONTENT_CHARSET));
        }
        return buff.append(']').toString();
    }
}
