/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class CheckOneDemandMessage implements Message {

    private final StockEntry candidate;

    CheckOneDemandMessage(final StockEntry candidate) {
        if (candidate == null) {
            throw new IllegalArgumentException("Null candidate.");
        }
        this.candidate = candidate;
    }

    StockEntry getCandidate() {
        return this.candidate;
    }

    @Override
    public int byteSize() {
        return this.candidate.byteSize();
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return this.candidate.toStream(output);
    }

    static BytesConvertible.Parser<CheckOneDemandMessage> getParser(final TypeRegistry<Chunk.Id<?>> idRegistry) {
        return new BytesConvertible.Parser<CheckOneDemandMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super CheckOneDemandMessage> output) throws MyRuleException,
                    IOException {
                final List<StockEntry> candidate = new ArrayList<>();
                final int size = StockEntry.getParser(idRegistry).fromStream(input, maxByteSize, candidate);
                output.add(new CheckOneDemandMessage(candidate.get(0)));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.candidate)
                .append(']').toString();
    }

    @Override
    public int hashCode() {
        return this.candidate.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof CheckOneDemandMessage)) {
            return false;
        }
        final CheckOneDemandMessage other = (CheckOneDemandMessage) obj;
        return this.candidate.equals(other.candidate);
    }
}
