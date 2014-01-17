/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 送信キューの集合体。
 * @author chirauraNoSakusha
 */
interface SendQueuePool {

    /**
     * 送信キューにメッセージを入れる。
     * @param destination 宛先
     * @param connectionType 通信路の種類
     * @param mail メッセージの中身
     * @return 新しくキューが作られた場合は true
     */
    boolean put(InetSocketAddress destination, int connectionType, List<Message> mail);

    /**
     * キューに入っているメッセージを取り出す。
     * メッセージが入っていない場合は待つ。
     * @param destination 宛先
     * @param connectionType 通信路の種類
     * @return 一番古いメッセージ
     * @throws InterruptedException 割り込まれた場合
     */
    List<Message> take(InetSocketAddress destination, int connectionType) throws InterruptedException;

    /**
     * キューに入っているメッセージを取り出す。
     * メッセージが入っていない場合は待機時間だけ待つ。
     * @param destination 宛先
     * @param connectionType 通信路の種類
     * @param waitMilliSeconds ミリ秒単位の待機時間
     * @return 一番古いメッセージ。
     *         待機時間まで待ってもメッセージが入って来なかった場合は null
     * @throws InterruptedException 割り込まれた場合
     */
    List<Message> take(InetSocketAddress destination, int connectionType, long waitMilliSeconds) throws InterruptedException;

    /**
     * キューに入っているメッセージを取り出す。
     * @param destination 宛先
     * @param connectionType 通信路の種類
     * @return 一番古いメッセージ。
     *         メッセージが無い場合は null
     */
    List<Message> takeIfExists(InetSocketAddress destination, int connectionType);

    /**
     * キューを追加する。
     * @param destination 宛先
     * @param connectionType 通信路の種類
     * @param connectionIdNumber 利用を始める接続のID
     * @return 新しくキューが作られた場合、つまり、まだ無かった場合は true
     */
    boolean addQueue(InetSocketAddress destination, int connectionType, int connectionIdNumber);

    /**
     * キューを削除する。
     * @param destination 宛先
     * @param connectionType 通信路の種類
     * @param connectionIdNumber 利用を終える接続のID
     * @return キューが削除された場合は残存メッセージ。
     *         キューが削除されなかった場合は null
     */
    List<List<Message>> removeQueue(InetSocketAddress destination, int connectionType, int connectionIdNumber);

    /**
     * キューに入っているメッセージの数を返す。
     * @param destination 宛先
     * @param connectionType 通信路の種類
     * @return キューに入っているメッセージの数。
     *         対応するキューが無い場合は負値
     */
    int size(InetSocketAddress destination, int connectionType);

    /**
     * キューの存在検査
     * @param destination 宛先
     * @param connectionType 通信路の種類
     * @return 有れば true
     */
    boolean containsQueue(InetSocketAddress destination, int connectionType);

}
