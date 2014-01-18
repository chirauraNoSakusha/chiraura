/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.util.EnumMap;
import java.util.Map;

/**
 * あったよー。
 * @author chirauraNoSakusha
 */
final class ContentResponse extends BasicResponse {

    private static Map<Http.Field, String> getFields(final Content content) {
        final Map<Http.Field, String> fields = new EnumMap<>(Http.Field.class);
        fields.put(Http.Field.LAST_MODIFIED, Http.formatDate(content.getUpdateDate()));
        fields.put(Http.Field.ETAG, Long.toString(content.getNetworkTag()));
        fields.put(Http.Field.CONTENT_TYPE, "text/plain; charset=" + Constants.CONTENT_CHARSET.name());
        return fields;
    }

    ContentResponse(final Content content) {
        super(Http.Status.OK, getFields(content), content.toNetworkString().getBytes(Constants.CONTENT_CHARSET));
    }

}
