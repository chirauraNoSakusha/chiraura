package nippon.kawauso.chiraura.bbs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nippon.kawauso.chiraura.lib.http.Http;

/**
 * この HTTP リクエスト何かおかしい。
 * @author chirauraNoSakusha
 */
final class BadRequestResponse extends CommentResponse {

    BadRequestResponse(final String comment) {
        super(Http.Status.Bad_Request, comment);
    }

    public static void main(final String[] args) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        (new BadRequestResponse("ばーか。")).toStream(output);
        System.out.println(new String(output.toByteArray(), Constants.CONTENT_CHARSET));
    }

}
