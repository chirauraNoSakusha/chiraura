package nippon.kawauso.chiraura.bbs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import nippon.kawauso.chiraura.lib.http.Http;

/**
 * 範囲指定が合わないよ。
 * @author chirauraNoSakusha
 */
final class RangeNotSatisfiableResponse extends BasicResponse {

    private static Map<Http.Field, String> getFields(final int contentLength) {
        final Map<Http.Field, String> fields = new EnumMap<>(Http.Field.class);
        fields.put(Http.Field.CONTENT_RANGE, (new StringBuilder("bytes */")).append(contentLength).toString());
        return fields;
    }

    RangeNotSatisfiableResponse(final String target, final int contentLength) {
        super(Http.Status.Requested_Range_Not_Satisfiable, getFields(contentLength), null);
        if (target == null) {
            throw new IllegalArgumentException("Null target.");
        }
    }

    public static void main(final String[] args) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        (new RangeNotSatisfiableResponse("unko", 2_000)).toStream(output);
        System.out.println(new String(output.toByteArray(), Constants.CONTENT_CHARSET));
    }

}
