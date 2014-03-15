package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class CheckStockReply implements Message {

    private final boolean rejected;
    private final boolean giveUp;

    private final List<StockEntry> stocks;

    private CheckStockReply(final boolean rejected, final boolean giveUp, final List<StockEntry> stocks) {
        this.giveUp = giveUp;
        this.rejected = rejected;
        this.stocks = stocks;
    }

    static CheckStockReply newRejected() {
        return new CheckStockReply(true, false, null);
    }

    static CheckStockReply newGiveUp() {
        return new CheckStockReply(false, true, null);
    }

    CheckStockReply(final List<StockEntry> stocked) {
        this(false, false, stocked);
        if (stocked == null) {
            throw new IllegalArgumentException("Null stocks entries.");
        }
    }

    boolean isRejected() {
        return this.rejected;
    }

    boolean isGivenUp() {
        return this.giveUp;
    }

    List<StockEntry> getStocks() {
        return this.stocks;
    }

    /*
     * 先頭バイトは、正常なら 1、諦めたなら 2、拒否ならそれ以外。
     */

    @Override
    public int byteSize() {
        if (this.rejected) {
            return 1;
        } else if (this.giveUp) {
            return 1;
        } else {
            return 1 + BytesConversion.byteSize("ao", this.stocks);
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.rejected) {
            output.write(0);
            return 1;
        } else if (this.giveUp) {
            output.write(2);
            return 1;
        } else {
            output.write(1);
            return 1 + BytesConversion.toStream(output, "ao", this.stocks);
        }
    }

    static BytesConvertible.Parser<CheckStockReply> getParser(final TypeRegistry<Chunk.Id<?>> idRegistry) {
        return new BytesConvertible.Parser<CheckStockReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super CheckStockReply> output) throws MyRuleException,
                    IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                int size = 1;
                if (flag[0] == 1) {
                    final List<StockEntry> stocked = new ArrayList<>();
                    size += BytesConversion.fromStream(input, maxByteSize - size, "ao", stocked, StockEntry.getParser(idRegistry));
                    output.add(new CheckStockReply(stocked));
                } else if (flag[0] == 2) {
                    output.add(CheckStockReply.newGiveUp());
                } else {
                    output.add(CheckStockReply.newRejected());
                }
                return size;
            }
        };
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName())).append('[');
        if (this.rejected) {
            buff.append("reject");
        } else if (this.giveUp) {
            buff.append("giveUp");
        } else {
            buff.append("numOfStocked=").append(this.stocks.size());
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.rejected ? 1231 : 1237);
        result = prime * result + (this.giveUp ? 1231 : 1237);
        result = prime * result + ((this.stocks == null) ? 0 : this.stocks.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof CheckStockReply)) {
            return false;
        }
        final CheckStockReply other = (CheckStockReply) obj;
        if (this.rejected != other.rejected || this.giveUp != other.giveUp) {
            return false;
        }
        if (this.stocks == null) {
            if (other.stocks != null) {
                return false;
            }
        } else if (!this.stocks.equals(other.stocks)) {
            return false;
        }
        return true;
    }

}
