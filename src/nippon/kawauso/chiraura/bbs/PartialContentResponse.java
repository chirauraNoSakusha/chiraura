/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * その部分あったよー。
 * @author chirauraNoSakusha
 */
final class PartialContentResponse extends BasicResponse {

    private static Map<Http.Field, String> getFields(final Content content, final byte[] contentBytes, final int rangeHead) {
        final Map<Http.Field, String> fields = new EnumMap<>(Http.Field.class);
        fields.put(Http.Field.LAST_MODIFIED, Http.formatDate(content.getUpdateDate()));
        fields.put(Http.Field.ETAG, Long.toString(content.getNetworkTag()));
        fields.put(Http.Field.CONTENT_TYPE, "text/plain; charset=" + Constants.CONTENT_CHARSET.name());
        fields.put(Http.Field.CONTENT_RANGE,
                "bytes " + Integer.toString(rangeHead) + "-" + Integer.toString(contentBytes.length - 1) + "/" + Integer.toString(contentBytes.length));
        return fields;
    }

    PartialContentResponse(final Content content, final byte[] contentBytes, final int rangeHead) {
        super(Http.Status.Partial_Content, getFields(content, contentBytes, rangeHead), Arrays.copyOfRange(contentBytes, rangeHead, contentBytes.length));
        if (content == null) {
            throw new IllegalArgumentException("Null content.");
        }
    }

    // 上のコンストラクタは下のコンストラクタより引数が冗長だし、整合性も保証できないが、toNetworkString しなくて済む。
    // PartialContentResponse(final Content content, final int rangeHead) {
    // this(content, content.toNetworkString().getBytes(Constants.CONTENT_CHARSET), rangeHead);
    // }

}
