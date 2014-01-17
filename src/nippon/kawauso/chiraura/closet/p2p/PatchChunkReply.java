/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.messenger.Message;

/**
 * @author chirauraNoSakusha
 */
final class PatchChunkReply implements Message {

    private final boolean rejected;
    private final boolean givenUp;
    private final boolean notFound;

    private final boolean success;

    PatchChunkReply(final boolean rejected, final boolean givenUp, final boolean notFound, final boolean success) {
        this.rejected = rejected;
        this.givenUp = givenUp;
        this.notFound = notFound;
        this.success = success;
    }

    static PatchChunkReply newRejected() {
        return new PatchChunkReply(true, false, false, false);
    }

    static PatchChunkReply newGiveUp() {
        return new PatchChunkReply(false, true, false, false);
    }

    static PatchChunkReply newNotFound() {
        return new PatchChunkReply(false, false, true, false);
    }

    static PatchChunkReply newFailure() {
        return new PatchChunkReply(false, false, false, false);
    }

    PatchChunkReply() {
        this(false, false, false, true);
    }

    boolean isRejected() {
        return this.rejected;
    }

    boolean isGivenUp() {
        return this.givenUp;
    }

    boolean isNotFound() {
        return this.notFound;
    }

    boolean isSuccess() {
        return this.success;
    }

    /*
     * 成功なら 1、失敗なら 2、無いなら 3、諦めてるなら 4、拒否ならそれ以外。
     */
    @Override
    public int byteSize() {
        return 1;
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.rejected) {
            output.write(0);
        } else if (this.givenUp) {
            output.write(4);
        } else if (this.notFound) {
            output.write(3);
        } else if (this.success) {
            output.write(1);
        } else {
            output.write(2);
        }
        return 1;
    }

    static BytesConvertible.Parser<PatchChunkReply> getParser() {
        return new BytesConvertible.Parser<PatchChunkReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super PatchChunkReply> output) throws MyRuleException,
                    IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                if (flag[0] == 1) {
                    output.add(new PatchChunkReply());
                } else if (flag[0] == 2) {
                    output.add(PatchChunkReply.newFailure());
                } else if (flag[0] == 3) {
                    output.add(PatchChunkReply.newNotFound());
                } else if (flag[0] == 4) {
                    output.add(PatchChunkReply.newGiveUp());
                } else {
                    output.add(PatchChunkReply.newRejected());
                }
                return 1;
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
        } else if (this.notFound) {
            buff.append("notFound");
        } else if (this.success) {
            buff.append("success");
        } else {
            buff.append("failure");
        }
        return buff.append(']').toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.rejected ? 1231 : 1237);
        result = prime * result + (this.givenUp ? 1231 : 1237);
        result = prime * result + (this.notFound ? 1231 : 1237);
        result = prime * result + (this.success ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof PatchChunkReply)) {
            return false;
        }
        final PatchChunkReply other = (PatchChunkReply) obj;
        return this.rejected == other.rejected && this.givenUp == other.givenUp && this.notFound == other.notFound && this.success == other.success;
    }

}
