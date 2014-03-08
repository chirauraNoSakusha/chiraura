/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.util.EnumMap;
import java.util.Map;

import nippon.kawauso.chiraura.lib.http.Http;

/**
 * あったよー。
 * @author chirauraNoSakusha
 */
class ContentResponse extends BasicResponse {

    private static Map<Http.Field, String> getFields(final Content content) {
        final Map<Http.Field, String> fields = new EnumMap<>(Http.Field.class);
        fields.put(Http.Field.LAST_MODIFIED, Http.formatDate(content.getUpdateDate()));
        fields.put(Http.Field.ETAG, Long.toString(content.getNetworkTag()));
        fields.put(Http.Field.CONTENT_TYPE, (new StringBuilder(content.getContentType())).append("; charset=").append(Constants.CONTENT_CHARSET.name())
                .toString());
        return fields;
    }

    ContentResponse(final Content content, final byte[] contentBytes) {
        super(Http.Status.OK, getFields(content), contentBytes);
    }

}
