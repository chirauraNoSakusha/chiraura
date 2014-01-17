/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.Message;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * @author chirauraNoSakusha
 */
final class CheckOneDemandReply implements Message {

    private final boolean rejected;
    private final boolean giveUp;

    private final DemandEntry demand;

    private CheckOneDemandReply(final boolean rejected, final boolean giveUp, final DemandEntry demand) {
        this.rejected = rejected;
        this.giveUp = giveUp;
        this.demand = demand;
    }

    static CheckOneDemandReply newRejected() {
        return new CheckOneDemandReply(true, false, null);
    }

    static CheckOneDemandReply newGiveUp() {
        return new CheckOneDemandReply(false, true, null);
    }

    static CheckOneDemandReply newNoDemand() {
        return new CheckOneDemandReply(false, false, null);
    }

    CheckOneDemandReply(final DemandEntry demand) {
        this(false, false, demand);
        if (demand == null) {
            throw new IllegalArgumentException("Null demand.");
        }
    }

    boolean isRejected() {
        return this.rejected;
    }

    boolean isGivenUp() {
        return this.giveUp;
    }

    boolean hasNoDemand() {
        return this.demand == null;
    }

    DemandEntry getDemand() {
        return this.demand;
    }

    /*
     * 先頭バイトは、需要があるなら 1、需要が無いなら 2、諦めたなら 3、拒否ならそれ以外。
     */

    @Override
    public int byteSize() {
        if (this.rejected) {
            return 1;
        } else if (this.giveUp) {
            return 1;
        } else if (this.demand == null) {
            return 1;
        } else {
            return 1 + this.demand.byteSize();
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.rejected) {
            output.write(0);
            return 1;
        } else if (this.giveUp) {
            output.write(3);
            return 1;
        } else if (this.demand == null) {
            output.write(2);
            return 1;
        } else {
            output.write(1);
            return 1 + this.demand.toStream(output);
        }
    }

    static BytesConvertible.Parser<CheckOneDemandReply> getParser(final TypeRegistry<Chunk.Id<?>> idRegistry) {
        return new BytesConvertible.Parser<CheckOneDemandReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super CheckOneDemandReply> output) throws MyRuleException,
                    IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                int size = 1;
                if (flag[0] == 1) {
                    final List<DemandEntry> demand = new ArrayList<>(1);
                    size += DemandEntry.getParser(idRegistry).fromStream(input, maxByteSize - size, demand);
                    output.add(new CheckOneDemandReply(demand.get(0)));
                } else if (flag[0] == 2) {
                    output.add(CheckOneDemandReply.newNoDemand());
                } else if (flag[0] == 3) {
                    output.add(CheckOneDemandReply.newGiveUp());
                } else {
                    output.add(CheckOneDemandReply.newRejected());
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
        } else if (this.demand == null) {
            buff.append("noDemand");
        } else {
            buff.append(this.demand);
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.rejected ? 1231 : 1237);
        result = prime * result + (this.giveUp ? 1231 : 1237);
        result = prime * result + ((this.demand == null) ? 0 : this.demand.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof CheckOneDemandReply)) {
            return false;
        }
        final CheckOneDemandReply other = (CheckOneDemandReply) obj;
        if (this.rejected != other.rejected || this.giveUp != other.giveUp) {
            return false;
        }
        if (this.demand == null) {
            if (other.demand != null) {
                return false;
            }
        } else if (!this.demand.equals(other.demand)) {
            return false;
        }
        return true;
    }

}
