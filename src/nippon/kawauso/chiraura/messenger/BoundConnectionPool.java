/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 接続情報の保存庫。
 * インスタンスを synchronized でロックすることにより、複数操作でも排他的に実行できる。
 * @author chirauraNoSakusha
 */
final class BoundConnectionPool<T extends BoundConnection> {

    private final Map<Integer, T> idToConnection;
    private final Map<InetAddress, Set<T>> clientToConnections;
    private final Map<InetSocketAddress, Set<T>> destinationToConnections;

    BoundConnectionPool() {
        this.idToConnection = new HashMap<>();
        this.clientToConnections = new HashMap<>();
        this.destinationToConnections = new HashMap<>();
    }

    /**
     * 空かどうか。
     * @return 空の場合のみ true
     */
    synchronized boolean isEmpty() {
        return this.idToConnection.isEmpty();
    }

    synchronized int getNumOfConnections(final InetAddress client) {
        final Set<T> family = this.clientToConnections.get(client);
        if (family == null) {
            return 0;
        } else {
            return family.size();
        }
    }

    synchronized int getNumOfConnections(final InetSocketAddress destination) {
        final Set<T> family = this.destinationToConnections.get(destination);
        if (family == null) {
            return 0;
        } else {
            return family.size();
        }
    }

    /**
     * 接続があるかどうか。
     * @param client 接続先
     * @return ある場合のみ true
     */
    synchronized boolean contains(final InetAddress client) {
        return this.clientToConnections.containsKey(client);
    }

    /**
     * 接続があるかどうか。
     * @param destination 接続先
     * @return ある場合のみ true
     */
    synchronized boolean contains(final InetSocketAddress destination) {
        return this.destinationToConnections.containsKey(destination);
    }

    synchronized List<T> get(final InetAddress client) {
        final Set<T> family = this.clientToConnections.get(client);
        if (family == null) {
            return new ArrayList<>(0);
        } else {
            return new ArrayList<>(family);
        }
    }

    /**
     * 通信相手を指定して接続を得る。
     * @param destination 通信相手
     * @return 接続
     */
    synchronized List<T> get(final InetSocketAddress destination) {
        final Set<T> family = this.destinationToConnections.get(destination);
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

        Set<T> family = this.clientToConnections.get(connection.getDestination().getAddress());
        if (family != null) {
            family.add(connection);
        } else {
            family = new HashSet<>();
            family.add(connection);
            this.clientToConnections.put(connection.getDestination().getAddress(), family);
        }

        family = this.destinationToConnections.get(connection.getDestination());
        if (family != null) {
            family.add(connection);
        } else {
            family = new HashSet<>();
            family.add(connection);
            this.destinationToConnections.put(connection.getDestination(), family);
        }
    }

    /**
     * 接続を消す。
     * @param idNumber 接続ID
     */
    synchronized void remove(final int idNumber) {
        final T connection = this.idToConnection.remove(idNumber);

        if (connection != null) {
            Set<T> family = this.clientToConnections.get(connection.getDestination().getAddress());
            family.remove(connection);
            if (family.isEmpty()) {
                this.clientToConnections.remove(connection.getDestination().getAddress());
            }

            family = this.destinationToConnections.get(connection.getDestination());
            family.remove(connection);
            if (family.isEmpty()) {
                this.destinationToConnections.remove(connection.getDestination());
            }
        }
    }

}
