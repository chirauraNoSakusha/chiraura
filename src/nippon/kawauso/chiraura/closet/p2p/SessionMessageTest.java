package nippon.kawauso.chiraura.closet.p2p;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

/**
 * @author chirauraNoSakusha
 */
public final class SessionMessageTest extends BytesConvertibleTest<SessionMessage> {

    @Override
    protected SessionMessage[] getInstances() {
        final List<SessionMessage> list = new ArrayList<>();
        list.add(new SessionMessage(new Session(1142)));
        list.add(new SessionMessage(new Session(0)));
        list.add(new SessionMessage(new Session(-1431241)));
        return list.toArray(new SessionMessage[0]);
    }

    @Override
    protected SessionMessage getInstance(final int seed) {
        return new SessionMessage(new Session(seed));
    }

    @Override
    protected BytesConvertible.Parser<SessionMessage> getParser() {
        return SessionMessage.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

}
