/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nippon.kawauso.chiraura.lib.http.Http;

/**
 * 何かエラー。
 * @author chirauraNoSakusha
 */
final class InternalServerErrorResponse extends CommentResponse {

    InternalServerErrorResponse(final String comment) {
        super(Http.Status.Internal_Server_Error, comment);
        if (comment == null) {
            throw new IllegalArgumentException("Null comment.");
        }
    }

    public static void main(final String[] args) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        (new InternalServerErrorResponse("ばーか。")).toStream(output);
        System.out.println(new String(output.toByteArray(), Constants.CONTENT_CHARSET));
    }

}
