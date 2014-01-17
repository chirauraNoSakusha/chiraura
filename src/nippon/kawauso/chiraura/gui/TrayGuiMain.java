/**
 * 
 */
package nippon.kawauso.chiraura.gui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author chirauraNoSakusha
 */
final class TrayGuiMain {

    public static void main(final String[] args) throws Exception {
        /*
         * 検査用。
         * メニューを表示させると死ねない環境がある。
         */
        final TrayGui instance = new TrayGui("/test/root", 111111);
        final ExecutorService executor = Executors.newCachedThreadPool();

        instance.start(executor);

        System.out.println("開始しました");

        Thread.sleep(10 * 1_000L);

        instance.close();

        System.out.println("終了しました");
    }

}
