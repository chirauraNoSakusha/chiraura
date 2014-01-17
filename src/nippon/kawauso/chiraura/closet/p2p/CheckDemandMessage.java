/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class CheckDemandMessage implements Message {

    private final Address start;
    private final Address end;
    private final List<StockEntry> candidates;

    CheckDemandMessage(final Address start, final Address end, final List<StockEntry> candidates) {
        if (start == null) {
            throw new IllegalArgumentException("Null start address.");
        } else if (end == null) {
            throw new IllegalArgumentException("Null end address.");
        } else if (candidates == null) {
            throw new IllegalArgumentException("Null candidates entries.");
        }
        this.start = start;
        this.end = end;
        this.candidates = candidates;
    }

    Address getStartAddress() {
        return this.start;
    }

    Address getEndAddress() {
        return this.end;
    }

    List<StockEntry> getCandidates() {
        return this.candidates;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("ooao", this.start, this.end, this.candidates);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "ooao", this.start, this.end, this.candidates);
    }

    static BytesConvertible.Parser<CheckDemandMessage> getParser(final TypeRegistry<Chunk.Id<?>> idRegistry) {
        return new BytesConvertible.Parser<CheckDemandMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super CheckDemandMessage> output) throws MyRuleException,
                    IOException {
                final List<Address> start = new ArrayList<>(1);
                final List<Address> end = new ArrayList<>(1);
                final List<StockEntry> candidates = new ArrayList<>();

                final BytesConvertible.Parser<Address> addressParser = Address.getParser();
                final int size = BytesConversion.fromStream(input, maxByteSize, "ooao", start, addressParser, end, addressParser,
                        candidates, StockEntry.getParser(idRegistry));

                output.add(new CheckDemandMessage(start.get(0), end.get(0), candidates));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append("[[").append(this.start)
                .append(", ").append(this.end)
                .append("], numOfCandidates=").append(Integer.toString(this.candidates.size()))
                .append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.start == null) ? 0 : this.start.hashCode());
        result = prime * result + ((this.end == null) ? 0 : this.end.hashCode());
        result = prime * result + ((this.candidates == null) ? 0 : this.candidates.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof CheckDemandMessage)) {
            return false;
        }
        final CheckDemandMessage other = (CheckDemandMessage) obj;
        return this.start.equals(other.start) && this.end.equals(other.end) && this.candidates.equals(other.candidates);
    }

}
