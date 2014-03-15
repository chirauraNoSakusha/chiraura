package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 他の個体とやり取りするための印。
 * @author chirauraNoSakusha
 */
final class Session implements BytesConvertible {

    private final long number;

    Session(final long number) {
        this.number = number;
    }

    @Override
    public int byteSize() {
        return NumberBytesConversion.byteSize(this.number);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return NumberBytesConversion.toStream(this.number, output);
    }

    static BytesConvertible.Parser<Session> getParser() {
        return new BytesConvertible.Parser<Session>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super Session> output) throws MyRuleException, IOException {
                final long[] id = new long[1];
                final int size = NumberBytesConversion.fromStream(input, maxByteSize, id);
                output.add(new Session(id[0]));
                return size;
            }
        };
    }

    @Override
    public int hashCode() {
        return (int) (this.number ^ (this.number >>> 32));
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Session)) {
            return false;
        }
        final Session other = (Session) obj;
        return this.number == other.number;
    }

    @Override
    public String toString() {
        return Long.toString(this.number);
    }

}
