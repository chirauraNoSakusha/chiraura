/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.logging.OneLineThreadFormatter;

/**
 * @author chirauraNoSakusha
 */
public final class TestFunctions {

    // インスタンス化防止。
    private TestFunctions() {}

    /**
     * デバッグフラグ
     */
    public static final boolean DEBUG = true;

    /**
     * 検査用のログ設定。
     * @param consoleLevel 画面出力
     * @param fileLevel ファイル出力
     * @param fileTag ログファイル名に使う文字列
     */
    public static void testLogging(final Level consoleLevel, final Level fileLevel, final String fileTag) {
        testLogging(Global.ROOT_LOGGER, consoleLevel, fileLevel, fileTag);
    }

    private static void testLogging(final Logger logger, final Level consoleLevel, final Level fileLevel, final String fileTag) {
        final Level level = (consoleLevel.intValue() < fileLevel.intValue() ? consoleLevel : fileLevel);
        synchronized (logger) {
            for (final Handler handler : logger.getHandlers()) {
                logger.removeHandler(handler);
                handler.close();
            }

            final List<Handler> handlers = new ArrayList<>();
            final Formatter formatter = new OneLineThreadFormatter();

            // 標準エラー出力。
            handlers.add(new ConsoleHandler());
            handlers.get(0).setLevel(consoleLevel);
            handlers.get(0).setFormatter(formatter);

            // ファイル。
            if (fileLevel != Level.OFF) {
                final String pattern = "tmp" + File.separator + fileTag + ".log";
                try {
                    handlers.add(new FileHandler(pattern, true));
                } catch (SecurityException | IOException e) {
                    throw new RuntimeException(e);
                }
                handlers.get(1).setLevel(fileLevel);
                handlers.get(1).setFormatter(formatter);
            }

            logger.setLevel(level);
            logger.setUseParentHandlers(false);
            for (final Handler handler : handlers) {
                logger.addHandler(handler);
            }
        }
    }

    /**
     * 検査用のログ設定。
     * @param fileTag ログファイル名に使う文字列
     */
    public static void testLogging(final String fileTag) {
        testLogging(Level.OFF, Level.SEVERE, fileTag);
    }

}
