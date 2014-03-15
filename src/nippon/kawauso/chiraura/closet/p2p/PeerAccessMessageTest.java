package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

/**
 * @author chirauraNoSakusha
 */
public final class PeerAccessMessageTest extends BytesConvertibleTest<PeerAccessMessage> {

    @Override
    protected PeerAccessMessage[] getInstances() {
        return new PeerAccessMessage[] { PeerAccessMessage.INSTANCE, PeerAccessMessage.INSTANCE, PeerAccessMessage.INSTANCE };
    }

    @Override
    protected PeerAccessMessage getInstance(final int seed) {
        return PeerAccessMessage.INSTANCE;
    }

    @Override
    protected BytesConvertible.Parser<PeerAccessMessage> getParser() {
        return PeerAccessMessage.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

}
