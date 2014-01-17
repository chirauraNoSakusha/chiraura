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
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.messenger.Message;

/**
 * @author chirauraNoSakusha
 */
final class AddCacheReply implements Message {

    private final boolean rejected;
    private final boolean givenUp;

    private final boolean success;
    private final long accessDate;

    private AddCacheReply(final boolean rejected, final boolean givenUp, final boolean success, final long accessDate) {
        this.rejected = rejected;
        this.givenUp = givenUp;
        this.success = success;
        this.accessDate = accessDate;
    }

    static AddCacheReply newRejected() {
        return new AddCacheReply(true, false, false, 0);
    }

    static AddCacheReply newGiveUp() {
        return new AddCacheReply(false, true, false, 0);
    }

    static AddCacheReply newFailure() {
        return new AddCacheReply(false, false, false, 0);
    }

    AddCacheReply(final long accessDate) {
        this(false, false, true, accessDate);
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

    long getAccessDate() {
        return this.accessDate;
    }

    /*
     * 成功なら 1、失敗なら 2、諦めてるなら 3、拒否ならそれ以外。
     */

    @Override
    public int byteSize() {
        if (this.rejected || this.givenUp) {
            return 1;
        } else if (this.success) {
            return 1 + NumberBytesConversion.byteSize(this.accessDate);
        } else {
            return 1;
        }
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        if (this.rejected) {
            output.write(0);
            return 1;
        } else if (this.givenUp) {
            output.write(3);
            return 1;
        } else if (this.success) {
            output.write(1);
            return 1 + NumberBytesConversion.toStream(this.accessDate, output);
        } else {
            output.write(2);
            return 1;
        }
    }

    static BytesConvertible.Parser<AddCacheReply> getParser() {
        return new BytesConvertible.Parser<AddCacheReply>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super AddCacheReply> output) throws MyRuleException,
                    IOException {
                if (maxByteSize < 1) {
                    throw new MyRuleException("Too short read limit ( " + maxByteSize + " ).");
                }
                final byte[] flag = StreamFunctions.completeRead(input, 1);
                int size = 1;
                if (flag[0] == 1) {
                    final long accessDate[] = new long[1];
                    size += NumberBytesConversion.fromStream(input, maxByteSize - size, accessDate);
                    output.add(new AddCacheReply(accessDate[0]));
                } else if (flag[0] == 2) {
                    output.add(AddCacheReply.newFailure());
                } else if (flag[0] == 3) {
                    output.add(AddCacheReply.newGiveUp());
                } else {
                    output.add(AddCacheReply.newRejected());
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
        } else if (this.success) {
            buff.append("success, ").append(LoggingFunctions.getSimpleDate(this.accessDate));
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
        result = prime * result + (int) (this.accessDate ^ (this.accessDate >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof AddCacheReply)) {
            return false;
        }
        final AddCacheReply other = (AddCacheReply) obj;
        return this.rejected == other.rejected && this.givenUp == other.givenUp && this.success == other.success && this.accessDate == other.accessDate;
    }

}
