/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 接続先のポートが不明な接続情報の保存庫。
 * @author chirauraNoSakusha
 */
final class ConnectionPool<T extends SkeletalConnection> {

    private final Map<Integer, T> idToConnection;
    private final Map<InetAddress, Set<T>> clientToConnections;

    ConnectionPool() {
        this.idToConnection = new HashMap<>();
        this.clientToConnections = new HashMap<>();
    }

    /**
     * 空かどうかを検査。
     * @return 空なら true
     */
    synchronized boolean isEmpty() {
        return this.idToConnection.isEmpty();
    }

    /**
     * 接続があるかどうか。
     * @param client 調べる接続先
     * @return ある場合のみ true
     */
    synchronized boolean contains(final InetAddress client) {
        return this.clientToConnections.containsKey(client);
    }

    synchronized int getNumOfConnections(final InetAddress client) {
        final Set<T> family = this.clientToConnections.get(client);
        if (family == null) {
            return 0;
        } else {
            return family.size();
        }
    }

    /**
     * 通信相手を指定して接続を得る。
     * @param client 通信相手
     * @return 接続。
     *         通信相手に対応する接続が無い場合は null
     */
    synchronized List<T> get(final InetAddress client) {
        final Set<T> family = this.clientToConnections.get(client);
        if (family == null) {
            return new ArrayList<>(0);
        } else {
            return new ArrayList<>(family);
        }
    }

    /**
     * 全ての接続を返す。
     * @return 全ての接続
     */
    synchronized List<T> getAll() {
        return new ArrayList<>(this.idToConnection.values());
    }

    /**
     * 接続を加える。
     * @param connection 追加する接続
     */
    synchronized void add(final T connection) {
        this.idToConnection.put(connection.getIdNumber(), connection);

        Set<T> family = this.clientToConnections.get(connection.getSocket().getInetAddress());
        if (family != null) {
            family.add(connection);
        } else {
            family = new HashSet<>();
            family.add(connection);
            this.clientToConnections.put(connection.getSocket().getInetAddress(), family);
        }
    }

    /**
     * 接続を消す。
     * @param idNumber 消す接続の識別番号
     */
    void remove(final int idNumber) {
        final T connection = this.idToConnection.remove(idNumber);

        if (connection != null) {
            final Set<T> family = this.clientToConnections.get(connection.getSocket().getInetAddress());
            family.remove(connection);
            if (family.isEmpty()) {
                this.clientToConnections.remove(connection.getSocket().getInetAddress());
            }
        }
    }

}
