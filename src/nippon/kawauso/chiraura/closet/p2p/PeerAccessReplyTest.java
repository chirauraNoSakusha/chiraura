/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.ArrayList;
import java.util.Arrays;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.network.AddressedPeer;
import nippon.kawauso.chiraura.network.AddressedPeerTest;

/**
 * @author chirauraNoSakusha
 */
public final class PeerAccessReplyTest extends UsingRegistryTest<PeerAccessReply> {

    @Override
    protected PeerAccessReply[] getInstances() {
        int seed = 0;
        return new PeerAccessReply[] {
                new PeerAccessReply(new ArrayList<AddressedPeer>(0)),
                new PeerAccessReply(Arrays.asList(AddressedPeerTest.newInstance(seed++))),
                new PeerAccessReply(Arrays.asList(AddressedPeerTest.newInstance(seed++), AddressedPeerTest.newInstance(seed++))),
                new PeerAccessReply(Arrays.asList(AddressedPeerTest.newInstance(seed++), AddressedPeerTest.newInstance(seed++),
                        AddressedPeerTest.newInstance(seed++))),
        };
    }

    @Override
    protected PeerAccessReply getInstance(final int seed) {
        return new PeerAccessReply(Arrays.asList(AddressedPeerTest.newInstance(seed), AddressedPeerTest.newInstance(seed + 1),
                AddressedPeerTest.newInstance(seed + 2)));
    }

    @Override
    protected BytesConvertible.Parser<PeerAccessReply> getParser() {
        return PeerAccessReply.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

    @Override
    protected int getNumOfExceptionLoops() {
        return 10 * getNumOfLoops();
    }

}
