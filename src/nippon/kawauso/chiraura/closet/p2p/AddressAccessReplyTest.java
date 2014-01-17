/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.Arrays;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.network.AddressedPeerTest;

/**
 * @author chirauraNoSakusha
 */
public final class AddressAccessReplyTest extends UsingRegistryTest<AddressAccessReply> {

    @Override
    protected AddressAccessReply[] getInstances() {
        int seed = 0;
        return new AddressAccessReply[] {
                AddressAccessReply.newRejected(),
                AddressAccessReply.newGiveUp(),
                new AddressAccessReply(null, Arrays.asList(AddressedPeerTest.newInstance(seed++), AddressedPeerTest.newInstance(seed++),
                        AddressedPeerTest.newInstance(seed++))),
                new AddressAccessReply(AddressedPeerTest.newInstance(seed++), Arrays.asList(AddressedPeerTest.newInstance(seed++),
                        AddressedPeerTest.newInstance(seed++), AddressedPeerTest.newInstance(seed++))),
        };
    }

    @Override
    protected AddressAccessReply getInstance(final int seed) {
        return new AddressAccessReply(AddressedPeerTest.newInstance(seed), Arrays.asList(AddressedPeerTest.newInstance(seed + 1),
                AddressedPeerTest.newInstance(seed + 2), AddressedPeerTest.newInstance(seed + 3)));
    }

    @Override
    protected BytesConvertible.Parser<AddressAccessReply> getParser() {
        return AddressAccessReply.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
