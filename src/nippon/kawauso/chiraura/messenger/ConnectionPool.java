/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author chirauraNoSakusha
 */
interface ConnectionPool<T extends SkeletalConnection> {

    /**
     * 空かどうか。
     * @return 空の場合のみ true
     */
    boolean isEmpty();

    /**
     * 接続の数を返す。
     * @param destination 接続先
     * @return 接続の数。
     */
    int getNumOfConnections(final InetSocketAddress destination);

    /**
     * 接続があるかどうか。
     * @param destination 接続先
     * @return ある場合のみ true
     */
    boolean contains(final InetSocketAddress destination);

    /**
     * 通信相手を指定して接続を得る。
     * @param destination 通信相手
     * @return 接続
     */
    List<T> get(final InetSocketAddress destination);

    /**
     * 全ての接続を返す。
     * @return 全ての接続
     */
    List<T> getAll();

    /**
     * 接続を加える。
     * @param connection 追加する接続
     */
    void add(final T connection);

    /**
     * 接続を消す。
     * @param idNumber 接続ID
     */
    void remove(final int idNumber);

}
