/**
 * 
 */
package nippon.kawauso.chiraura.gui;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;

/**
 * @author chirauraNoSakusha
 */
public final class TrayGuiTest {

    /**
     * 起動試験。
     * @throws Exception 異常
     */
    // @Test
    public void testBoot() throws Exception {
        LoggingFunctions.startDebugLogging();

        final TrayGui instance = new TrayGui("/test/root", 111111, 0L, 10_000L, 30_000L);

        final ExecutorService executor = Executors.newCachedThreadPool();

        final long interval = 10 * 1_000L;

        instance.start(executor);
        System.out.println("起動しました");
        Thread.sleep(interval);

        instance.displayClosePortWarning(22222);
        System.out.println("ポート異常を設定しました");
        Thread.sleep(interval);

        instance.setSelf(new InetSocketAddress(InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 }), 22222), "1234567890");
        System.out.println("自分を設定しました");
        Thread.sleep(interval);

        instance.displayVersionGapWarning(1, 3);
        System.out.println("バージョン異常を設定しました");
        Thread.sleep(interval);

        instance.displayVersionGapWarning(1, 0);
        System.out.println("バージョン異常を弱めました");
        Thread.sleep(interval);

        instance.displayVersionGapWarning(2, 0);
        System.out.println("バージョン異常を強めました");
        Thread.sleep(interval);

        instance.displayJceError();
        System.out.println("JCE異常を設定しました");
        Thread.sleep(interval);

        instance.displayServerError();
        System.out.println("サーバ異常を設定しました");
        Thread.sleep(interval);

        instance.displayClosePortWarning(44444);
        System.out.println("ポート異常を変更しました");
        Thread.sleep(interval);

        instance.setSelf(new InetSocketAddress(InetAddress.getByAddress(new byte[] { 5, 6, 7, 8 }), 44444), "abcdefghijk");
        System.out.println("自分を変更しました");
        Thread.sleep(interval);

        Thread.sleep(60_000L);

        instance.close();
    }

}
