/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
final class UnknownMessage implements Message {

    private final long id;
    private final int size;

    private UnknownMessage(final long id, final int size) {
        this.id = id;
        this.size = size;
    }

    long getId() {
        return this.id;
    }

    int getSize() {
        return this.size;
    }

    @Override
    public int byteSize() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    static BytesConvertible.Parser<UnknownMessage> getParser(final long id) {
        return new BytesConvertible.Parser<UnknownMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super UnknownMessage> output) throws MyRuleException,
                    IOException {
                StreamFunctions.completeSkip(input, maxByteSize);
                output.add(new UnknownMessage(id, maxByteSize));
                return maxByteSize;
            }
        };
    }

}
