package nippon.kawauso.chiraura.closet.p2p;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

/**
 * @author chirauraNoSakusha
 */
public final class SessionTest extends BytesConvertibleTest<Session> {

    @Override
    protected Session[] getInstances() {
        final List<Session> list = new ArrayList<>();
        list.add(new Session(0));
        list.add(new Session(2313452));
        list.add(new Session(-34));
        return list.toArray(new Session[0]);
    }

    @Override
    protected Session getInstance(final int seed) {
        return new Session(seed);
    }

    @Override
    protected BytesConvertible.Parser<Session> getParser() {
        return Session.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

}
