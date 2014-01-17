/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 送信キューの実装。
 * @author chirauraNoSakusha
 */
final class BasicSendQueuePool implements SendQueuePool {

    /*
     * removeFlags にキューを利用する接続のIDが登録されていない場合にのみ、
     * 接続を削除するようにする。
     */
    private final BlockingQueuePool<ConnectionGroupId, List<Message>> queuePool;

    /*
     * キューを利用する接続のIDを登録する。
     */
    private final Map<ConnectionGroupId, Set<Integer>> removeFlags;

    BasicSendQueuePool() {
        this.queuePool = new BlockingQueuePool<>();
        this.removeFlags = new HashMap<>();
    }

    @Override
    public boolean put(final InetSocketAddress destination, final int connectionType, final List<Message> mail) {
        return this.queuePool.put(new ConnectionGroupId(destination, connectionType), mail);
    }

    @Override
    public List<Message> take(final InetSocketAddress destination, final int connectionType) throws InterruptedException {
        return this.queuePool.take(new ConnectionGroupId(destination, connectionType));
    }

    @Override
    public List<Message> take(final InetSocketAddress destination, final int connectionType, final long waitMilliSeconds) throws InterruptedException {
        return this.queuePool.take(new ConnectionGroupId(destination, connectionType), waitMilliSeconds);
    }

    @Override
    public List<Message> takeIfExists(final InetSocketAddress destination, final int connectionType) {
        return this.queuePool.takeIfExists(new ConnectionGroupId(destination, connectionType));
    }

    @Override
    public boolean addQueue(final InetSocketAddress destination, final int connectionType, final int connectionIdNumber) {
        final ConnectionGroupId group = new ConnectionGroupId(destination, connectionType);
        synchronized (this.removeFlags) {
            Set<Integer> groupUsers = this.removeFlags.get(group);
            if (groupUsers == null) {
                groupUsers = new HashSet<>();
                this.removeFlags.put(group, groupUsers);
            }
            groupUsers.add(connectionIdNumber);
            return this.queuePool.addQueue(group);
        }
    }

    @Override
    public List<List<Message>> removeQueue(final InetSocketAddress destination, final int connectionType, final int connectionIdNumber) {
        final ConnectionGroupId group = new ConnectionGroupId(destination, connectionType);
        synchronized (this.removeFlags) {
            Set<Integer> groupUsers = this.removeFlags.get(group);
            if (groupUsers != null) {
                groupUsers.remove(connectionIdNumber);
                if (groupUsers.isEmpty()) {
                    this.removeFlags.remove(group);
                    groupUsers = null;
                }
            }
            if (groupUsers == null) {
                return this.queuePool.removeQueue(group);
            } else {
                return null;
            }
        }
    }

    @Override
    public int size(final InetSocketAddress destination, final int connectionType) {
        return this.queuePool.size(new ConnectionGroupId(destination, connectionType));
    }

    @Override
    public boolean containsQueue(final InetSocketAddress destination, final int connectionType) {
        return this.queuePool.conteinsQueue(new ConnectionGroupId(destination, connectionType));
    }

}
