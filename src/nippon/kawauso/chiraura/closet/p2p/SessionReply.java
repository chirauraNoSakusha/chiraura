package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.Message;

/**
 * 受信したやり取り用の印を示す言付け。
 * @author chirauraNoSakusha
 */
final class SessionReply implements Message {

    private final Session session;

    SessionReply(final Session session) {
        if (session == null) {
            throw new IllegalArgumentException("Null session.");
        }
        this.session = session;
    }

    Session get() {
        return this.session;
    }

    @Override
    public int byteSize() {
        return this.session.byteSize();
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return this.session.toStream(output);
    }

    static BytesConvertible.Parser<SessionReply> getParser() {
        return new BytesConvertible.Parser<SessionReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super SessionReply> output) throws MyRuleException,
                    IOException {
                final List<Session> session = new ArrayList<>(1);
                final int size = Session.getParser().fromStream(input, maxByteSize, session);
                output.add(new SessionReply(session.get(0)));
                return size;
            }
        };
    }

    @Override
    public int hashCode() {
        return this.session.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof SessionReply)) {
            return false;
        }
        final SessionReply other = (SessionReply) obj;
        return this.session.equals(other.session);
    }

}
