/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.gui;

import java.net.InetAddress;
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
     * @param source 通信相手
     */
    public void setSelf(InetSocketAddress self, String publicString, InetAddress source);

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
     * @param source 通信相手
     */
    public void displayClosePortWarning(int port, InetAddress source);

    /**
     * より新しい個体がいることを表示する。
     * @param majorGap メジャーバージョンの差
     * @param minorGap マイナーバージョンの差
     */
    public void displayVersionGapWarning(long majorGap, long minorGap);

}
