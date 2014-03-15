package nippon.kawauso.chiraura.closet.p2p;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

/**
 * @author chirauraNoSakusha
 */
public final class SessionReplyMessageTest extends BytesConvertibleTest<SessionReply> {

    @Override
    protected SessionReply[] getInstances() {
        final List<SessionReply> list = new ArrayList<>();
        list.add(new SessionReply(new Session(1142)));
        list.add(new SessionReply(new Session(0)));
        list.add(new SessionReply(new Session(-1431241)));
        return list.toArray(new SessionReply[0]);
    }

    @Override
    protected SessionReply getInstance(final int seed) {
        return new SessionReply(new Session(seed));
    }

    @Override
    protected BytesConvertible.Parser<SessionReply> getParser() {
        return SessionReply.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

}
