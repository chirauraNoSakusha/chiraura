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
final class AddChunkReply implements Message {

    private final boolean rejected;
    private final boolean givenUp;

    private final boolean success;

    private AddChunkReply(final boolean rejected, final boolean giveUp, final boolean success) {
        this.rejected = rejected;
        this.givenUp = giveUp;
        this.success = success;
    }

    static AddChunkReply newRejected() {
        return new AddChunkReply(true, false, false);
    }

    static AddChunkReply newGiveUp() {
        return new AddChunkReply(false, true, false);
    }

    static AddChunkReply newFailure() {
        return new AddChunkReply(false, false, false);
    }

    AddChunkReply() {
        this(false, false, true);
    }

    boolean isRejected() {
        return this.rejected;
    }

    boolean isGivenUp() {
        return this.givenUp;
    }

    boolean isSuccess() {
        return this.success;
    }

    /*
     * 成功なら 1、失敗なら 2、諦めてるなら 3、拒否ならそれ以外。
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
            output.write(3);
        } else if (this.success) {
            output.write(1);
        } else {
            output.write(2);
        }
        return 1;
    }

    static BytesConvertible.Parser<AddChunkReply> getParser() {
        return new BytesConvertible.Parser<AddChunkReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super AddChunkReply> output) throws MyRuleException, IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                if (flag[0] == 1) {
                    output.add(new AddChunkReply());
                } else if (flag[0] == 2) {
                    output.add(AddChunkReply.newFailure());
                } else if (flag[0] == 3) {
                    output.add(AddChunkReply.newGiveUp());
                } else {
                    output.add(AddChunkReply.newRejected());
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
        result = prime * result + (this.success ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof AddChunkReply)) {
            return false;
        }
        final AddChunkReply other = (AddChunkReply) obj;
        return this.rejected == other.rejected && this.givenUp == other.givenUp && this.success == other.success;
    }

}
