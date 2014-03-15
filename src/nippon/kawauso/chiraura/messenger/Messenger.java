package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.ExecutorService;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * 通信係。
 * メッセージの配達と受け取りをやる。
 * @author chirauraNoSakusha
 */
public interface Messenger {

    /**
     * 自身の識別用公開鍵を返す。
     * @return 自身の識別用公開鍵
     */
    public KeyPair getId();

    /**
     * 他個体から見た自身の情報を返す。
     * @return 他個体から見た自身
     */
    public InetSocketAddress getSelf();

    /**
     * メッセージを送信する。
     * @param destination 宛先
     * @param connectionType 使用する接続の種類
     * @param mail 内容
     */
    public void send(InetSocketAddress destination, int connectionType, List<Message> mail);

    /**
     * 受信したメッセージを取り出す。
     * 受信したメッセージが無いときは受信するまで待つ。
     * @return 最も早く受信が完了したメッセージ
     * @throws InterruptedException 割り込まれた場合
     */
    public ReceivedMail take() throws InterruptedException;

    /**
     * 受信したメッセージを取り出す。
     * @return 最も早く受信が完了したメッセージ。
     *         受信したメッセージが無いときは null
     */
    public ReceivedMail takeIfExists();

    /**
     * 接続があるかどうか調べる。
     * @param destination 調べる接続先
     * @return 接続があった場合のみ true
     */
    public boolean containsConnection(InetSocketAddress destination);

    /**
     * 接続をブチ切る。
     * @param destination ブチ切る接続の宛先
     * @return ブチ切る接続があった場合のみ true
     */
    public boolean removeConnection(InetSocketAddress destination);

    /**
     * 受信と送信を始める。
     * @param executor 実行機
     */
    public void start(ExecutorService executor);

    /**
     * メッセージの送受信中に発生したエラーを取り出す。
     * エラーが無いときは発生するまで待つ。
     * @return 最も古いエラー
     * @throws InterruptedException 割り込まれた場合
     */
    public MessengerReport takeReport() throws InterruptedException;

    /**
     * メッセージの送受信中に発生したエラーを取り出す。
     * @return 最も古いエラー。
     *         エラーが無いときは null
     */
    public MessengerReport takeReportIfExists();

    /**
     * メッセージを登録する。
     * @param <T> 登録するメッセージのクラス
     * @param typeId 固有番号
     * @param type 登録するメッセージのクラス
     * @param parser 復号器
     */
    public <T extends Message> void registerMessage(long typeId, Class<T> type, BytesConvertible.Parser<? extends T> parser);

}
