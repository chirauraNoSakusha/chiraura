/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.security.KeyPair;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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

    private static AddressableNetwork sampleNetwork(final Random random) {
        final Address self = AddressTest.newRandomInstance(random);
        final int peerCapacity = 1_000;
        final long maintenanceInterval = 10_000;
        return AddressableNetworks.newInstance(self, peerCapacity, maintenanceInterval);
    }

    private static Messenger sampleMessenger(@SuppressWarnings("unused") final Random random) {
        final int port = 12345;
        final int receivBufferSize = 1024;
        final int sendBufferSize = 1024;
        final long connectionTimeout = 60 * 1_000L;
        final long operationTimeout = 10 * 1_000L;
        final int messageSizeLimit = 1024 * 1024 + 1024;
        final KeyPair id = CryptographicKeys.newPublicKeyPair();
        final long version = 1;
        final long publicKeyLifetime = 60 * 60 * 1_000L;
        final long commonKeyLifetime = 10 * 60 * 1_000L;
        return Messengers.newInstance(port, receivBufferSize, sendBufferSize, connectionTimeout, operationTimeout, messageSizeLimit, id, version,
                publicKeyLifetime, commonKeyLifetime);
    }

    static NetworkWrapper sample(final Random random, final AddressCalculator calculator) {
        final long version = 1;
        final AddressableNetwork network = sampleNetwork(random);
        final Messenger messenger = sampleMessenger(random);
        final PeerBlacklist blacklist = new TimeLimitedPeerBlacklist(1_000, 30_000L);
        final PeerBlacklist lostPeers = new TimeLimitedPeerBlacklist(1_000, 3_000L);
        final PeerPot pot = new FifoPeerPot(1_000);
        final BlockingQueue<Operation> operationQueue = new LinkedBlockingQueue<>();
        final int activeAddressLogLimit = 1_000;
        final long activeAddressDuration = 60 * 1_000L;
        return new NetworkWrapper(version, network, messenger, blacklist, lostPeers, pot, operationQueue, calculator, activeAddressLogLimit,
                activeAddressDuration);
    }

}
