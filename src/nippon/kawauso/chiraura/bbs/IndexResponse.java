/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.util.EnumMap;
import java.util.Map;

/**
 * 板直下 HTML の TITLE タグで板名を取得する 2ch ブラウザに対応するためだけにある。
 * @author chirauraNoSakusha
 */
final class IndexResponse extends BasicResponse {

    private static Map<Http.Field, String> getFields() {
        final Map<Http.Field, String> fields = new EnumMap<>(Http.Field.class);
        fields.put(Http.Field.CONTENT_TYPE, "text/html; charset=" + Constants.CONTENT_CHARSET.name());
        return fields;
    }

    IndexResponse(final String board, final String label) {
        super(Http.Status.OK, getFields(),
                (new StringBuilder("<HTML>"))
                        .append("<HEAD>")
                        .append("<title>").append(label).append("</title>") // 2chMate は小文字タグしか対応してないっぽい。
                        .append("</HEAD>")
                        .append("<BODY>")
                        .append('/').append(board).append('/').append(Constants.BOARD_FILE).append(" を利用してください。")
                        .append("</BODY>")
                        .append("</HTML>").toString().getBytes(Constants.CONTENT_CHARSET));
        if (board == null) {
            throw new IllegalArgumentException("Null board.");
        }
    }

    IndexResponse(final String board) {
        this(board, board);
    }
}
