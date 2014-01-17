/**
 * 
 */
package nippon.kawauso.chiraura.gui;

import java.net.InetSocketAddress;

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

        final TrayGui instance = new TrayGui("/test/root", 111111);

        final long interval = 5 * 1_000L;

        instance.start(null);
        System.out.println("起動しました");
        Thread.sleep(interval);

        instance.displayClosePortWarning(22222);
        System.out.println("ポート異常を設定しました");
        Thread.sleep(interval);

        instance.setSelf(new InetSocketAddress(22222), "^1234567890");
        System.out.println("自分を設定しました");
        Thread.sleep(interval);

        instance.displayJceError();
        System.out.println("JCE異常を設定しました");
        Thread.sleep(interval);

        instance.displayServerError();
        System.out.println("サーバ異常を設定しました");
        Thread.sleep(interval);

        instance.displayNewProtocolWarning(1, 3);
        System.out.println("バージョン異常を設定しました");
        Thread.sleep(interval);

        instance.setSelf(null, null);
        System.out.println("自分を解除しました");
        Thread.sleep(interval);

        instance.setSelf(new InetSocketAddress(22222), "^1234567890");
        System.out.println("自分を設定しました");
        Thread.sleep(interval);

        Thread.sleep(30_000L);

        instance.close();
    }

}
