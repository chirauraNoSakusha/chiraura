package nippon.kawauso.chiraura.bbs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nippon.kawauso.chiraura.lib.http.Http;

/**
 * 変わってないよー。
 * @author chirauraNoSakusha
 */
final class NotModifiedResponse extends BasicResponse {

    NotModifiedResponse(final String target) {
        super(Http.Status.Not_Modified, null, null);
        if (target == null) {
            throw new IllegalArgumentException("Null target.");
        }
    }

    public static void main(final String[] args) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        (new NotModifiedResponse("unko")).toStream(output);
        System.out.println(new String(output.toByteArray(), Constants.CONTENT_CHARSET));
    }

}
