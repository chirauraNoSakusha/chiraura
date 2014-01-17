/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.container.Pair;

/**
 * 調整を加えた Chord ネットワーク。
 * 以下、自身の論理位置を基点とする。
 * 論理空間で基点から次の個体の位置の手前までを領土とする。
 * 近傍個体として、基点からから次の個体、その次の個体、さらにその次の個体、さらにさらにその次の個体、と個体数の log の数の個体を把握する。
 * 転送先として、基点から2のべき乗だけ離れた位置を領土とする個体を把握する。
 * @author chirauraNoSakusha
 */
final class CustomChord implements AddressableNetwork {

    // 保持。
    private final CcView view;
    private final BlockingQueue<NetworkTask> taskQueue;

    // 実行用引数。
    private final long maintenanceInterval;

    CustomChord(final Address self, final int peerCapacity, final long maintenanceInterval) {
        if (self == null) {
            throw new IllegalArgumentException("Null self.");
        } else if (peerCapacity < 0) {
            throw new IllegalArgumentException("Negative peer capacity ( " + peerCapacity + " ).");
        } else if (maintenanceInterval < 0) {
            throw new IllegalArgumentException("Negative maintenance interval ( " + maintenanceInterval + " ).");
        }

        this.view = new BasicCcView(self, peerCapacity);
        this.taskQueue = new LinkedBlockingQueue<>();

        this.maintenanceInterval = maintenanceInterval;
    }

    @Override
    public Address getSelf() {
        return this.view.getBase();
    }

    @Override
    public Pair<Address, Address> getDomain() {
        return this.view.getDomain();
    }

    @Override
    public boolean isEmpty() {
        return this.view.isEmpty();
    }

    @Override
    public boolean dominates(final Address target) {
        return this.view.dominates(target);
    }

    @Override
    public boolean moreAppropriate(final Address target, final Address competitor) {
        return this.view.getBase().distanceTo(target).compareTo(competitor.distanceTo(target)) < 0;
    }

    @Override
    public AddressedPeer getRoutingDestination(final Address target) {
        return this.view.getRoutingDestination(target);
    }

    @Override
    public List<AddressedPeer> getRoutingNeighbors(final int maxHop) {
        return this.view.getSuccessors(maxHop);
    }

    @Override
    public List<AddressedPeer> getBackupNeighbors(final int maxHop) {
        return this.view.getPredecessors(maxHop);
    }

    @Override
    public List<AddressedPeer> getImportantPeers() {
        return this.view.getImportantPeers();
    }

    @Override
    public List<AddressedPeer> getShortcuts() {
        return this.view.getFingers();
    }

    @Override
    public List<AddressedPeer> getPeers() {
        return this.view.getPeers();
    }

    @Override
    public boolean addPeer(final AddressedPeer peer) {
        return this.view.addPeer(peer);
    }

    @Override
    public Address removePeer(final InetSocketAddress peer) {
        return this.view.removePeer(peer);
    }

    @Override
    public void start(final ExecutorService executor) {
        executor.submit(new CcBoss(executor, this.view, this.maintenanceInterval, this.taskQueue));
    }

    @Override
    public NetworkTask take() throws InterruptedException {
        return this.taskQueue.take();
    }

    @Override
    public NetworkTask takeIfExists() {
        return this.taskQueue.poll();
    }

}
