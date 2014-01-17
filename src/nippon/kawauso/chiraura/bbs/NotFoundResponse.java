/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 無いよー。
 * @author chirauraNoSakusha
 */
final class NotFoundResponse extends CommentResponse {

    NotFoundResponse(final String target) {
        super(Http.Status.Not_Found, (new StringBuilder(target)).append(" が見つかりません。").toString());
        if (target == null) {
            throw new IllegalArgumentException("Null target.");
        }
    }

    public static void main(final String[] args) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        (new NotFoundResponse("himitsu/maruhi")).toStream(output);
        System.out.println(new String(output.toByteArray(), Constants.CONTENT_CHARSET));
    }

}
