package nippon.kawauso.chiraura.bbs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 接続情報の保存庫。
 * @author chirauraNoSakusha
 */
final class ConnectionPool {

    private final Set<Connection> container;

    ConnectionPool() {
        this.container = new HashSet<>();
    }

    synchronized void add(final Connection connection) {
        this.container.add(connection);
    }

    synchronized void remove(final Connection connection) {
        this.container.remove(connection);
    }

    synchronized List<Connection> getAll() {
        return new ArrayList<>(this.container);
    }

}
