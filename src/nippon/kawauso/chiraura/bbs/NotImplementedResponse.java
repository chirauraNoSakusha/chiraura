/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 2chブラウザからの許されないリクエスト。
 * @author chirauraNoSakusha
 */
final class NotImplementedResponse extends BasicResponse {

    NotImplementedResponse(final Http.Method method) {
        super(Http.Status.Not_Implemented, null, null);
        if (method == null) {
            throw new IllegalArgumentException("Null method.");
        }
    }

    public static void main(final String[] args) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        (new NotImplementedResponse(Http.Method.HEAD)).toStream(output);
        System.out.println(new String(output.toByteArray(), Constants.CONTENT_CHARSET));
    }

}
