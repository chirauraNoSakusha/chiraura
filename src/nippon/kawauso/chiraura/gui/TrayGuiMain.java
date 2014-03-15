package nippon.kawauso.chiraura.gui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;

/**
 * @author chirauraNoSakusha
 */
final class TrayGuiMain {

    public static void main(final String[] args) throws InterruptedException {
        /*
         * 検査用。
         * メニューを表示させると死ねない環境がある。
         */

        LoggingFunctions.startDebugLogging();

        final TrayGui instance = new TrayGui("/test/root", 111111, Duration.SECOND, Duration.SECOND, 10);
        final ExecutorService executor = Executors.newCachedThreadPool();

        instance.start(executor);

        System.out.println("開始しました");

        instance.take();

        instance.close();

        System.out.println("終了しました");
    }

}
