package nippon.kawauso.chiraura.messenger;

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
final class BoundConnectionPool<T extends BoundConnection> implements ConnectionPool<T> {

    private final Map<Integer, T> idToConnection;
    private final Map<InetSocketAddress, Set<T>> destinationToConnections;

    BoundConnectionPool() {
        this.idToConnection = new HashMap<>();
        this.destinationToConnections = new HashMap<>();
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.idToConnection.isEmpty();
    }

    @Override
    public synchronized int getNumOfConnections(final InetSocketAddress destination) {
        final Set<T> family = this.destinationToConnections.get(destination);
        if (family == null) {
            return 0;
        } else {
            return family.size();
        }
    }

    @Override
    public synchronized boolean contains(final InetSocketAddress destination) {
        return this.destinationToConnections.containsKey(destination);
    }

    @Override
    public synchronized List<T> get(final InetSocketAddress destination) {
        final Set<T> family = this.destinationToConnections.get(destination);
        if (family == null) {
            return new ArrayList<>(0);
        } else {
            return new ArrayList<>(family);
        }
    }

    @Override
    public synchronized List<T> getAll() {
        return new ArrayList<>(this.idToConnection.values());
    }

    @Override
    public synchronized void add(final T connection) {
        this.idToConnection.put(connection.getIdNumber(), connection);

        final Set<T> family = this.destinationToConnections.get(connection.getDestination());
        if (family != null) {
            family.add(connection);
        } else {
            final Set<T> newFamily = new HashSet<>();
            newFamily.add(connection);
            this.destinationToConnections.put(connection.getDestination(), newFamily);
        }
    }

    @Override
    public synchronized void remove(final int idNumber) {
        final T connection = this.idToConnection.remove(idNumber);

        if (connection != null) {
            final Set<T> family = this.destinationToConnections.get(connection.getDestination());
            family.remove(connection);
            if (family.isEmpty()) {
                this.destinationToConnections.remove(connection.getDestination());
            }
        }
    }

}
