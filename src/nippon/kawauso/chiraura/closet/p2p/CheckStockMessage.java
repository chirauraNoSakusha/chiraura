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
final class CheckStockMessage implements Message {

    private final Address start;
    private final Address end;
    private final List<StockEntry> exclusives;

    CheckStockMessage(final Address start, final Address end, final List<StockEntry> exclusives) {
        if (start == null) {
            throw new IllegalArgumentException("Null start address.");
        } else if (end == null) {
            throw new IllegalArgumentException("Null end address.");
        } else if (exclusives == null) {
            throw new IllegalArgumentException("Null exclusives entries.");
        }
        this.start = start;
        this.end = end;
        this.exclusives = exclusives;
    }

    Address getStartAddress() {
        return this.start;
    }

    Address getEndAddress() {
        return this.end;
    }

    List<StockEntry> getExclusives() {
        return this.exclusives;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("ooao", this.start, this.end, this.exclusives);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "ooao", this.start, this.end, this.exclusives);
    }

    static BytesConvertible.Parser<CheckStockMessage> getParser(final TypeRegistry<Chunk.Id<?>> idRegistry) {
        return new BytesConvertible.Parser<CheckStockMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super CheckStockMessage> output) throws MyRuleException,
                    IOException {
                final List<Address> startAddress = new ArrayList<>(1);
                final List<Address> endAddress = new ArrayList<>(1);
                final List<StockEntry> exclusiveEntries = new ArrayList<>();

                final BytesConvertible.Parser<Address> addressParser = Address.getParser();
                final int size = BytesConversion.fromStream(input, maxByteSize, "ooao", startAddress, addressParser, endAddress, addressParser,
                        exclusiveEntries, StockEntry.getParser(idRegistry));

                output.add(new CheckStockMessage(startAddress.get(0), endAddress.get(0), exclusiveEntries));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append("[[").append(this.start)
                .append(", ").append(this.end)
                .append("], numOfExclusive=").append(this.exclusives.size())
                .append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.start.hashCode();
        result = prime * result + this.end.hashCode();
        result = prime * result + this.exclusives.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof CheckStockMessage)) {
            return false;
        }
        final CheckStockMessage other = (CheckStockMessage) obj;
        return this.start.equals(other.start) && this.end.equals(other.end) && this.exclusives.equals(other.exclusives);
    }

}
