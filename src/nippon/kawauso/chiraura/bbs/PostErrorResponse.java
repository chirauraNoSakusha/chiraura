package nippon.kawauso.chiraura.bbs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author chirauraNoSakusha
 */
final class PostErrorResponse extends PostResponse {

    PostErrorResponse(final String title, final String comment) {
        super(Post.Result.ERROR, "ＥＲＲＯＲ：" + title, comment);
    }

    public static void main(final String[] args) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        (new PostErrorResponse("ばーか。", "死んでください。")).toStream(output);
        System.out.println(new String(output.toByteArray(), Constants.CONTENT_CHARSET));
    }

}
