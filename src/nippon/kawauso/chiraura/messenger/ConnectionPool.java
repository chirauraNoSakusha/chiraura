/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接続先が不明な接続情報の保存庫。
 * @author chirauraNoSakusha
 */
final class ConnectionPool<T extends SkeletalConnection> {

    private final Map<Integer, T> idToConnection;

    ConnectionPool() {
        this.idToConnection = new ConcurrentHashMap<>();
    }

    /**
     * 空かどうかを検査。
     * @return 空なら true
     */
    boolean isEmpty() {
        return this.idToConnection.isEmpty();
    }

    /**
     * 接続があるかどうか。
     * @param destination 調べる接続先
     * @return ある場合のみ true
     */
    boolean contains(final InetSocketAddress destination) {
        // TODO InetAddress だけを利用する方法も考えられるが保留。
        return false;
    }

    /**
     * 通信相手を指定して接続を得る。
     * @param destination 通信相手
     * @return 接続。
     *         通信相手に対応する接続が無い場合は null
     */
    List<T> get(final InetSocketAddress destination) {
        // TODO InetAddress だけを利用する方法も考えられるが保留。
        return new ArrayList<>(0);
    }

    /**
     * 全ての接続を返す。
     * @return 全ての接続
     */
    List<T> getAll() {
        return new ArrayList<>(this.idToConnection.values());
    }

    /**
     * 接続を加える。
     * @param connection 追加する接続
     */
    void add(final T connection) {
        this.idToConnection.put(connection.getIdNumber(), connection);
    }

    /**
     * 接続を消す。
     * @param idNumber 消す接続の識別番号
     */
    void remove(final int idNumber) {
        this.idToConnection.remove(idNumber);
    }

}
