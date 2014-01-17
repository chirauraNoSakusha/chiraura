/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.Global;

/**
 * @author chirauraNoSakusha
 */
public final class LoggingFunctions {

    // インスタンス化防止。
    private LoggingFunctions() {}

    /**
     * @param date 日時 (ミリ秒)
     * @return シンプルな日時表現
     */
    public static String getSimpleDate(final long date) {
        return String.format("%1$tY/%1$tm/%1$td %1$tH:%1$tM:%1$tS.%1$tL", date);
    }

    /**
     * @param date 日時 (ミリ秒)
     * @return 短い時刻表現
     */
    public static String getShortDate(final long date) {
        return String.format("%1$ty.%1$tm.%1$td.%1$tH.%1$tM.%1$tS", date);
    }

    /**
     * Throwable.printStackTrace()で表示されるのと同じ文字列をつくる。
     * @param throwable nullは駄目。
     * @return エラーを示す文字列
     */
    public static String getStackTraceString(final Throwable throwable) {
        final StringWriter sink = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sink, true));
        return sink.toString();
    }

    /**
     * ログを標準出力するように設定する。
     * @param logger 設定するロガー
     * @param level 出力する最低ログレベル
     */
    public static void startLogging(final Logger logger, final Level level) {
        synchronized (logger) {
            for (final Handler remove : logger.getHandlers()) {
                logger.removeHandler(remove);
                remove.close();
            }

            final Handler handler = new ConsoleHandler();
            handler.setLevel(level);
            handler.setFormatter(new OneLineThreadFormatter());

            logger.setLevel(level);
            logger.setUseParentHandlers(false);
            logger.addHandler(handler);
        }
    }

    /**
     * ログを標準出力するように設定する。
     * @param level 出力する最低ログレベル
     */
    public static void startLogging(final Level level) {
        startLogging(Global.ROOT_LOGGER, level);
    }

    /**
     * 標準のログを標準出力するように設定する。
     */
    public static void startLogging() {
        startLogging(Level.INFO);
    }

    /**
     * デバッグ向けのログを標準出力するように設定する。
     */
    public static void startDebugLogging() {
        startLogging(Level.ALL);
    }

}
