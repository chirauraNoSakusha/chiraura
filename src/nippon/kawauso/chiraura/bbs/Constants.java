package nippon.kawauso.chiraura.bbs;

import java.nio.charset.Charset;

/**
 * @author chirauraNoSakusha
 */
final class Constants {

    // インスタンス化防止。
    private Constants() {}

    static final Charset CONTENT_CHARSET = Charset.forName("Shift_JIS");

    static final String SERVER_LABEL = "chiraura-bbs/1";

    static final String BOARD_FILE = "subject.txt";

}
