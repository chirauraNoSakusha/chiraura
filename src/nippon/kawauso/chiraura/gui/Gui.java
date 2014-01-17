/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.gui;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

/**
 * @author chirauraNoSakusha
 */
public interface Gui extends AutoCloseable {

    /**
     * 入力された仕事を取り出す。
     * 無いときは待つ。
     * @return 入力された仕事
     * @throws InterruptedException 割り込まれた場合
     */
    public GuiCommand take() throws InterruptedException;

    /**
     * 開始する。
     * @param executor 実行機
     */
    public void start(ExecutorService executor);

    @Override
    public void close();

    /**
     * 自分の個体情報を変更する。
     * @param self 個体情報
     * @param publicString 公開形式の個体情報
     */
    public void setSelf(InetSocketAddress self, String publicString);

    /**
     * JCE が制限されていることを表示する。
     */
    public void displayJceError();

    /**
     * サーバが起動できないことを表示する。
     */
    public void displayServerError();

    /**
     * ポートが閉じているかもしれないことを表示する。
     * @param port 問題のポート番号
     */
    public void displayClosePortWarning(int port);

    /**
     * 動作規約の新しい版が出ていることを表示する。
     * @param version 自分の版
     * @param newVersion 新しい版
     */
    public void displayNewProtocolWarning(long version, long newVersion);

}
