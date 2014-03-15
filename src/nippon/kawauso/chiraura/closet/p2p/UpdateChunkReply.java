package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.Message;

/**
 * @author chirauraNoSakusha
 */
final class UpdateChunkReply implements Message {

    private final boolean rejected;
    private final boolean givenUp;

    private final long type;
    private final List<? extends Mountain.Dust<?>> diffs;

    private UpdateChunkReply(final boolean rejected, final boolean givenUp, final long type, final List<? extends Mountain.Dust<?>> diffs) {
        this.rejected = rejected;
        this.givenUp = givenUp;
        this.type = type;
        this.diffs = diffs;
    }

    static UpdateChunkReply newRejected() {
        return new UpdateChunkReply(true, false, 0, null);
    }

    static UpdateChunkReply newGiveUp() {
        return new UpdateChunkReply(false, true, 0, null);
    }

    static UpdateChunkReply newNotFound() {
        return new UpdateChunkReply(false, false, 0, null);
    }

    UpdateChunkReply(final TypeRegistry<Mountain.Dust<?>> diffRegistry, final List<? extends Mountain.Dust<?>> diffs) {
        this(false, false, (diffs.isEmpty() ? 0 : diffRegistry.getId(diffs.get(0))), diffs);
    }

    boolean isRejected() {
        return this.rejected;
    }

    boolean isGivenUp() {
        return this.givenUp;
    }

    boolean isNotFound() {
        return this.diffs == null;
    }

    List<? extends Mountain.Dust<?>> getDiffs() {
        return this.diffs;
    }

    /*
     * 先頭バイトは、更新があったら 1、無かったら 2、そもそもデータ片が無かったら 3、諦めてるなら 4、拒否ならそれ以外。
     */

    @Override
    public int byteSize() {
        if (this.rejected) {
            return 1;
        } else if (this.givenUp) {
            return 1;
        } else if (this.diffs == null) {
            return 1;
        } else if (this.diffs.isEmpty()) {
            return 1;
        } else {
            return 1 + BytesConversion.byteSize("lao", this.type, this.diffs);
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.rejected) {
            output.write(0);
            return 1;
        } else if (this.givenUp) {
            output.write(4);
            return 1;
        } else if (this.diffs == null) {
            output.write(3);
            return 1;
        } else if (this.diffs.isEmpty()) {
            output.write(2);
            return 1;
        } else {
            output.write(1);
            return 1 + BytesConversion.toStream(output, "lao", this.type, this.diffs);
        }
    }

    static BytesConvertible.Parser<UpdateChunkReply> getParser(final TypeRegistry<Mountain.Dust<?>> diffRegistry) {
        return new BytesConvertible.Parser<UpdateChunkReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super UpdateChunkReply> output) throws MyRuleException,
                    IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                int size = 1;
                if (flag[0] == 1) {
                    final long[] type = new long[1];
                    final List<? extends Mountain.Dust<?>> diffs = new ArrayList<>();
                    size += NumberBytesConversion.fromStream(input, maxByteSize - size, type);
                    final BytesConvertible.Parser<? extends Mountain.Dust<?>> parser = diffRegistry.getParser(type[0]);
                    if (parser == null) {
                        throw new MyRuleException("Not registered diff type ( " + type[0] + " ).");
                    }
                    size += BytesConversion.fromStream(input, maxByteSize - size, "ao", diffs, parser);
                    output.add(new UpdateChunkReply(diffRegistry, diffs));
                } else if (flag[0] == 2) {
                    output.add(new UpdateChunkReply(diffRegistry, new ArrayList<Mountain.Dust<?>>(0)));
                } else if (flag[0] == 3) {
                    output.add(UpdateChunkReply.newNotFound());
                } else if (flag[0] == 4) {
                    output.add(UpdateChunkReply.newGiveUp());
                } else {
                    output.add(UpdateChunkReply.newRejected());
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
        } else if (this.givenUp) {
            buff.append("giveUp");
        } else if (this.diffs == null) {
            buff.append("notFound");
        } else {
            buff.append("numOfDiffs=").append(this.diffs.size());
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.rejected ? 1231 : 1237);
        result = prime * result + (this.givenUp ? 1231 : 1237);
        result = prime * result + ((this.diffs == null) ? 0 : this.diffs.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof UpdateChunkReply)) {
            return false;
        }
        final UpdateChunkReply other = (UpdateChunkReply) obj;
        if (this.rejected != other.rejected || this.givenUp != other.givenUp) {
            return false;
        }
        if (this.diffs == null) {
            if (other.diffs != null) {
                return false;
            }
        } else if (!this.diffs.equals(other.diffs)) {
            return false;
        }
        return true;
    }

}
