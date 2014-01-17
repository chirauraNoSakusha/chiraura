/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author chirauraNoSakusha
 */
abstract class CommentResponse extends BasicResponse {

    private static Map<Http.Field, String> getFields() {
        final Map<Http.Field, String> fields = new EnumMap<>(Http.Field.class);
        fields.put(Http.Field.CONTENT_TYPE, "text/html; charset=" + Constants.CONTENT_CHARSET.name());
        return fields;
    }

    CommentResponse(final Http.Status status, final String comment) {
        super(status, getFields(),
                (new StringBuilder("<HTML>"))
                        .append("<HEAD>")
                        .append("<TITLE>").append(status).append("</TITLE>")
                        .append("</HEAD>")
                        .append("<BODY>")
                        .append(comment)
                        .append("</BODY>")
                        .append("</HTML>").toString().getBytes(Constants.CONTENT_CHARSET));
        if (comment == null) {
            throw new IllegalArgumentException("Null comment.");
        }
    }

}
