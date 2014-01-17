/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author chirauraNoSakusha
 */
class PostResponse extends BasicResponse {

    private static Map<Http.Field, String> getFields() {
        final Map<Http.Field, String> fields = new EnumMap<>(Http.Field.class);
        fields.put(Http.Field.CONTENT_TYPE, "text/html; charset=" + Constants.CONTENT_CHARSET.name());
        return fields;
    }

    PostResponse(final String title, final String comment) {
        super(Http.Status.OK, getFields(),
                (new StringBuilder("<HTML>"))
                        .append("<HEAD>")
                        .append("<TITLE>").append(title).append("</TITLE>")
                        .append("</HEAD>")
                        .append("<BODY>")
                        .append(comment)
                        .append("</BODY>")
                        .append("</HTML>").toString().getBytes(Constants.CONTENT_CHARSET));
        if (title == null) {
            throw new IllegalArgumentException("Null title.");
        } else if (comment == null) {
            throw new IllegalArgumentException("Null comment.");
        }
    }

    public static void main(final String[] args) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        (new PostResponse("ばーか。", "死んでください。")).toStream(output);
        System.out.println(new String(output.toByteArray(), Constants.CONTENT_CHARSET));
    }

}
