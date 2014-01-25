/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.container.Pair;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.messenger.Messenger;
import nippon.kawauso.chiraura.messenger.MessengerReport;
import nippon.kawauso.chiraura.messenger.ReceivedMail;
import nippon.kawauso.chiraura.network.AddressableNetwork;
import nippon.kawauso.chiraura.network.AddressedPeer;
import nippon.kawauso.chiraura.network.NetworkTask;

/**
 * 四次元押し入れ用に取り繕った通信系。
 * @author chirauraNoSakusha
 */
final class NetworkWrapper {

    private static final Logger LOG = Logger.getLogger(NetworkWrapper.class.getName());

    private final long version;
    private final AddressableNetwork network;
    private final Messenger messenger;
    private final PeerBlacklist blacklist;
    private final PeerBlacklist lostPeers;
    private final PeerPot backup;

    private final BlockingQueue<Operation> operationSink;

    private final AddressCalculator calculator;

    private final AddressLog activeAddressLog;

    NetworkWrapper(final long version, final AddressableNetwork network, final Messenger messenger, final PeerBlacklist blacklist,
            final PeerBlacklist lostPeers, final PeerPot pot, final BlockingQueue<Operation> operationSink, final AddressCalculator calculator,
            final int activeAddressLogLimit, final long activeAddressDuration) {
        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (messenger == null) {
            throw new IllegalArgumentException("Null messenger.");
        } else if (blacklist == null) {
            throw new IllegalArgumentException("Null black list.");
        } else if (lostPeers == null) {
            throw new IllegalArgumentException("Null lost peers.");
        } else if (pot == null) {
            throw new IllegalArgumentException("Null backup.");
        } else if (operationSink == null) {
            throw new IllegalArgumentException("Null operation sink.");
        } else if (calculator == null) {
            throw new IllegalArgumentException("Null calculator.");
        } else if (activeAddressLogLimit < 0) {
            throw new IllegalArgumentException("Negative active address log limit ( " + activeAddressLogLimit + " ).");
        } else if (activeAddressDuration < 0) {
            throw new IllegalArgumentException("Negative active address duration ( " + activeAddressDuration + " ).");
        }

        this.version = version;
        this.network = network;
        this.messenger = messenger;
        this.blacklist = blacklist;
        this.lostPeers = lostPeers;
        this.backup = pot;

        this.operationSink = operationSink;

        this.calculator = calculator;

        this.activeAddressLog = new AddressLog(activeAddressLogLimit, activeAddressDuration);
    }

    /**
     * 現在のプロトコルバージョンを返す。
     * @return プロトコルバージョン
     */
    long getVersion() {
        return this.version;
    }

    Address getSelfAddress() {
        return this.network.getSelf();
    }

    Pair<Address, Address> getDomain() {
        return this.network.getDomain();
    }

    boolean isEmpty() {
        return this.network.isEmpty();
    }

    boolean dominates(final Address target) {
        return this.network.dominates(target);
    }

    boolean moreAppropriate(final Address target, final PublicKey competitorId) {
        final Address competitor = this.calculator.calculate(competitorId);
        return this.network.moreAppropriate(target, competitor);
    }

    AddressedPeer getRoutingDestination(final Address target) {
        return this.network.getRoutingDestination(target);
    }

    List<AddressedPeer> getShortcuts() {
        return this.network.getShortcuts();
    }

    List<AddressedPeer> getRoutingNeighbors(final int maxHop) {
        return this.network.getRoutingNeighbors(maxHop);
    }

    List<AddressedPeer> getBackupNeighbors(final int maxHop) {
        return this.network.getBackupNeighbors(maxHop);
    }

    List<AddressedPeer> getImportantPeers() {
        return this.network.getImportantPeers();
    }

    List<AddressedPeer> getPeers() {
        return this.network.getPeers();
    }

    boolean addPeer(final AddressedPeer peer) {
        if (this.blacklist.contains(peer.getPeer()) || this.lostPeers.contains(peer.getPeer())) {
            return false;
        } else if (this.activeAddressLog.containsAndNotEquals(peer.getPeer(), peer.getAddress())) {
            // 別の論理位置で登録されている場合はスルー。
            return false;
        } else {
            this.backup.put(peer.getPeer());
            if (this.network.addPeer(peer)) {
                // 個体の確認。
                ConcurrentFunctions.completePut(new PeerAccessOperation(peer.getPeer()), this.operationSink);
                return true;
            } else {
                return false;
            }
        }
    }

    boolean addActivePeer(final PublicKey peerId, final InetSocketAddress peer) {
        if (this.blacklist.contains(peer)) {
            // 除外対象は切断。
            this.messenger.removeConnection(peer);
            return false;
        } else {
            final Address address = this.calculator.calculate(peerId);
            this.lostPeers.remove(peer);
            this.backup.put(peer);
            this.activeAddressLog.add(peer, address);
            return this.network.addPeer(new AddressedPeer(address, peer));
        }
    }

    boolean removePeer(final InetAddress peer) {
        LOG.log(Level.INFO, "未実装なので {0} は削除されません。", peer);
        return false;
    }

    boolean removeInvalidPeer(final InetAddress peer) {
        LOG.log(Level.INFO, "未実装なので {0} は削除されません。", peer);
        return false;
    }

    boolean removePeer(final InetSocketAddress peer) {
        final Address removed = this.network.removePeer(peer);
        this.messenger.removeConnection(peer);
        this.activeAddressLog.remove(peer);
        if (removed != null) {
            // TODO 個体を補完すべきか？
            // ConcurrentFunctions.completePut(new AddressAccessOperation(removed), this.operationSink);
            return true;
        } else {
            return false;
        }
    }

    boolean removeLostPeer(final InetSocketAddress peer) {
        this.lostPeers.add(peer);
        return removePeer(peer);
    }

    boolean removeInvalidPeer(final InetSocketAddress peer) {
        this.blacklist.add(peer);
        return removePeer(peer);
    }

    NetworkTask takeNetworkTask() throws InterruptedException {
        return this.network.take();
    }

    NetworkTask takeNetworkTaskIfExists() {
        return this.network.takeIfExists();
    }

    KeyPair getId() {
        return this.messenger.getId();
    }

    InetSocketAddress getSelf() {
        return this.messenger.getSelf();
    }

    void sendMail(final InetSocketAddress destination, final int connectionType, final List<Message> mail) {
        this.messenger.send(destination, connectionType, mail);
    }

    ReceivedMail takeReceivedMail() throws InterruptedException {
        return this.messenger.take();
    }

    ReceivedMail takeReceivedMailIfExists() {
        return this.messenger.takeIfExists();
    }

    boolean containsConnection(final InetSocketAddress destination) {
        return this.messenger.containsConnection(destination);
    }

    boolean removeConnection(final InetSocketAddress destination) {
        return this.messenger.removeConnection(destination);
    }

    MessengerReport takeMessengerReport() throws InterruptedException {
        return this.messenger.takeReport();
    }

    MessengerReport takeMessengerReportIfExists() {
        return this.messenger.takeReportIfExists();
    }

    boolean inBlacklist(final InetSocketAddress peer) {
        return this.blacklist.contains(peer) || this.lostPeers.contains(peer);
    }

    InetSocketAddress getReservedPeer() {
        return this.backup.get();
    }

    List<InetSocketAddress> getReservedPeers() {
        return this.backup.getAll();
    }

    void reservePeer(final InetSocketAddress peer) {
        this.backup.put(peer);
    }

    void start(final ExecutorService executor) {
        this.network.start(executor);
        this.messenger.start(executor);
    }

    <T extends Message> void registerMessage(final long id, final Class<T> type, final BytesConvertible.Parser<? extends T> parser) {
        this.messenger.registerMessage(id, type, parser);
    }

}
