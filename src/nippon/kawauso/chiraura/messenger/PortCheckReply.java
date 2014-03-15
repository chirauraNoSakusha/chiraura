package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
final class PortCheckReply implements Message {

    private final int value;

    private PortCheckReply(final int value) {
        this.value = value;
    }

    PortCheckReply(final byte[] watchword) {
        this(Arrays.hashCode(watchword));
        if (watchword == null) {
            throw new IllegalArgumentException("Null watchword.");
        }
    }

    int getValue() {
        return this.value;
    }

    @Override
    public int byteSize() {
        return NumberBytesConversion.byteSize(this.value);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return NumberBytesConversion.toStream(this.value, output);
    }

    static BytesConvertible.Parser<PortCheckReply> getParser() {
        return new BytesConvertible.Parser<PortCheckReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super PortCheckReply> output) throws MyRuleException,
                    IOException {
                final int[] value = new int[1];
                final int size = NumberBytesConversion.intFromStream(input, maxByteSize, value);
                output.add(new PortCheckReply(value[0]));
                return size;
            }
        };
    }

    @Override
    public int hashCode() {
        return this.value;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof PortCheckReply)) {
            return false;
        }
        final PortCheckReply other = (PortCheckReply) obj;
        return this.value == other.value;
    }

}
