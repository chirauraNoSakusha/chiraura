/**
 * 
 */
package nippon.kawauso.chiraura;

import java.nio.charset.Charset;
import java.util.logging.Logger;

/**
 * @author chirauraNoSakusha
 */
public final class Global {

    /**
     * デバッグ中かどうか。
     * @return デバッグ中なら true
     */
    public static boolean isDebug() {
        return true;
    }

    /**
     * 内部で使う文字コード。
     */
    public static final Charset INTERNAL_CHARSET = Charset.forName("UTF-8");

    /**
     * ロガーの一斉設定用。
     */
    public static final Logger ROOT_LOGGER = Logger.getLogger(Global.class.getPackage().getName());

}
