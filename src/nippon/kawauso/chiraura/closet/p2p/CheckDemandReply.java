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
final class CheckDemandReply implements Message {

    private final boolean rejected;
    private final boolean giveUp;

    private final List<DemandEntry> demands;

    private CheckDemandReply(final boolean rejected, final boolean giveUp, final List<DemandEntry> demands) {
        this.rejected = rejected;
        this.giveUp = giveUp;
        this.demands = demands;
    }

    static CheckDemandReply newRejected() {
        return new CheckDemandReply(true, false, null);
    }

    static CheckDemandReply newGiveUp() {
        return new CheckDemandReply(false, true, null);
    }

    CheckDemandReply(final List<DemandEntry> demanded) {
        this(false, false, demanded);
        if (demanded == null) {
            throw new IllegalArgumentException("Null demands entries.");
        }
    }

    boolean isRejected() {
        return this.rejected;
    }

    boolean isGivenUp() {
        return this.giveUp;
    }

    List<DemandEntry> getDemands() {
        return this.demands;
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
            return 1 + BytesConversion.byteSize("ao", this.demands);
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
            return 1 + BytesConversion.toStream(output, "ao", this.demands);
        }
    }

    static BytesConvertible.Parser<CheckDemandReply> getParser(final TypeRegistry<Chunk.Id<?>> idRegistry) {
        return new BytesConvertible.Parser<CheckDemandReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super CheckDemandReply> output) throws MyRuleException,
                    IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                int size = 1;
                if (flag[0] == 1) {
                    final List<DemandEntry> demanded = new ArrayList<>();
                    size += BytesConversion.fromStream(input, maxByteSize - size, "ao", demanded, DemandEntry.getParser(idRegistry));
                    output.add(new CheckDemandReply(demanded));
                } else if (flag[0] == 2) {
                    output.add(CheckDemandReply.newGiveUp());
                } else {
                    output.add(CheckDemandReply.newRejected());
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
            buff.append("numOfDemanded=").append(Integer.toBinaryString(this.demands.size()));
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.rejected ? 1231 : 1237);
        result = prime * result + (this.giveUp ? 1231 : 1237);
        result = prime * result + ((this.demands == null) ? 0 : this.demands.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof CheckDemandReply)) {
            return false;
        }
        final CheckDemandReply other = (CheckDemandReply) obj;
        if (this.rejected != other.rejected || this.giveUp != other.giveUp) {
            return false;
        }
        if (this.demands == null) {
            if (other.demands != null) {
                return false;
            }
        } else if (!this.demands.equals(other.demands)) {
            return false;
        }
        return true;
    }

}
