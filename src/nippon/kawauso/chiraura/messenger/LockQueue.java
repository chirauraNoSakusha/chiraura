/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.util.List;

/**
 * ロック結合して使うためのブロッキングキュー。
 * @author chirauraNoSakusha
 */
interface LockQueue<E> {

    /**
     * ロックする。
     */
    void lock();

    /**
     * ロックを外す。
     */
    void unlock();

    /**
     * 要素を入れる。
     * @param element 入れる要素
     */
    void put(E element);

    /**
     * 要素を取り出す。
     * 要素が無い場合は入れられるまで待つ。
     * @return 先頭の要素
     * @throws InterruptedException インタラプトされた場合
     */
    E take() throws InterruptedException;

    /**
     * 要素を取り出す。
     * 要素が無い場合は制限時間まで入れられるまで待つ。
     * @param waitMilliSeconds 制限時間 (ミリ秒)
     * @return 先頭の要素。
     *         制限時間切れの場合は null
     * @throws InterruptedException インタラプトされた場合
     */
    E take(long waitMilliSeconds) throws InterruptedException;

    /**
     * 要素を取り出す。
     * @return 先頭の要素
     *         要素が入っていない場合は null
     */
    E takeIfExists();

    /**
     * 全ての要素を取り出す。
     * @return 全ての要素
     */
    List<E> removeAll();

    /**
     * 要素数を返す。
     * @return 要素数
     */
    int size();

}
