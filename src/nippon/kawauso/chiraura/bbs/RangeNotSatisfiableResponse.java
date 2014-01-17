/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 範囲指定が合わないよ。
 * @author chirauraNoSakusha
 */
final class RangeNotSatisfiableResponse extends BasicResponse {

    RangeNotSatisfiableResponse(final String target) {
        super(Http.Status.Not_Modified, null, null);
        if (target == null) {
            throw new IllegalArgumentException("Null target.");
        }
    }

    public static void main(final String[] args) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        (new RangeNotSatisfiableResponse("unko")).toStream(output);
        System.out.println(new String(output.toByteArray(), Constants.CONTENT_CHARSET));
    }

}
