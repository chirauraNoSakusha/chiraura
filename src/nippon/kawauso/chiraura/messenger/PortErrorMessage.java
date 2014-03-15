package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
final class PortErrorMessage implements Message {

    private static final int PAD_SIZE = 10;

    @Override
    public int byteSize() {
        return PAD_SIZE;
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        final byte[] pad = new byte[PAD_SIZE];
        ThreadLocalRandom.current().nextBytes(pad);
        output.write(pad);
        return pad.length;
    }

    static BytesConvertible.Parser<PortErrorMessage> getParser() {
        return new BytesConvertible.Parser<PortErrorMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super PortErrorMessage> output) throws MyRuleException,
                    IOException {
                if (maxByteSize < PAD_SIZE) {
                    throw new MyRuleException("Read limit ( " + maxByteSize + " ) smaller than required size ( " + PAD_SIZE + " ).");
                }
                StreamFunctions.completeRead(input, PAD_SIZE);
                output.add(new PortErrorMessage());
                return PAD_SIZE;
            }
        };
    }

}
