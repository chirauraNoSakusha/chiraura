/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.security.KeyPair;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.AddressTest;
import nippon.kawauso.chiraura.messenger.CryptographicKeys;
import nippon.kawauso.chiraura.messenger.Messenger;
import nippon.kawauso.chiraura.messenger.Messengers;
import nippon.kawauso.chiraura.network.AddressableNetwork;
import nippon.kawauso.chiraura.network.AddressableNetworks;

/**
 * @author chirauraNoSakusha
 */
public final class NetworkWrapperTest {

    private static final long duration = Duration.SECOND / 2;
    private static final long sizeLimit = 10_000_000L;
    private static final int countLimit = 1_000;
    private static final long penalty = 5 * Duration.SECOND;

    private static AddressableNetwork sampleNetwork(final Random random) {
        final Address self = AddressTest.newRandomInstance(random);
        final int peerCapacity = 1_000;
        final long maintenanceInterval = 10 * Duration.SECOND;
        return AddressableNetworks.newInstance(self, peerCapacity, maintenanceInterval);
    }

    private static Messenger sampleMessenger(@SuppressWarnings("unused") final Random random) {
        final int port = 12345;
        final int receivBufferSize = 1024;
        final int sendBufferSize = 1024;
        final long connectionTimeout = Duration.MINUTE;
        final long operationTimeout = 10 * Duration.SECOND;
        final int messageSizeLimit = 1024 * 1024 + 1024;
        final KeyPair id = CryptographicKeys.newPublicKeyPair();
        final long version = 1L;
        final long versionGapThreshold = 1L;
        final long publicKeyLifetime = Duration.HOUR;
        final long commonKeyLifetime = 10 * Duration.MINUTE;
        return Messengers.newInstance(port, receivBufferSize, sendBufferSize, connectionTimeout, operationTimeout, messageSizeLimit, version,
                versionGapThreshold, id, publicKeyLifetime, commonKeyLifetime, duration, sizeLimit, countLimit, penalty);
    }

    static NetworkWrapper sample(final Random random, final AddressCalculator calculator) {
        final long version = 1;
        final AddressableNetwork network = sampleNetwork(random);
        final Messenger messenger = sampleMessenger(random);
        final PeerBlacklist blacklist = new TimeLimitedPeerBlacklist(1_000, 30 * Duration.SECOND);
        final PeerBlacklist lostPeers = new TimeLimitedPeerBlacklist(1_000, 3 * Duration.SECOND);
        final PeerPot pot = new FifoPeerPot(1_000);
        final BlockingQueue<Operation> operationQueue = new LinkedBlockingQueue<>();
        final int activeAddressLogLimit = 1_000;
        final long activeAddressDuration = Duration.MINUTE;
        return new NetworkWrapper(version, network, messenger, blacklist, lostPeers, pot, operationQueue, calculator, activeAddressLogLimit,
                activeAddressDuration);
    }

}
