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
final class ForbiddenResponse extends CommentResponse {

    ForbiddenResponse(final String target) {
        super(Http.Status.Forbidden, (new StringBuilder("ディレクトリ ")).append(target).append(" の読み込みは許可されていません。").toString());
        if (target == null) {
            throw new IllegalArgumentException("Null target.");
        }
    }

    public static void main(final String[] args) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        (new ForbiddenResponse("/test/")).toStream(output);
        System.out.println(new String(output.toByteArray(), Constants.CONTENT_CHARSET));
    }

}
