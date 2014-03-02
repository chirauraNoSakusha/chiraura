/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.cell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
public final class LongCell implements BytesConvertible {

    private final long value;

    /**
     * @param value 任意のlong値
     */
    public LongCell(final long value) {
        this.value = value;
    }

    /**
     * @return 設定されている値
     */
    public long get() {
        return this.value;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("cl", this.value);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "cl", this.value);
    }

    /**
     * @return 復号器
     */
    public static BytesConvertible.Parser<LongCell> getParser() {
        return new BytesConvertible.Parser<LongCell>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super LongCell> output) throws MyRuleException, IOException {
                final long[] buff = new long[1];
                final int size = BytesConversion.fromStream(input, maxByteSize, "cl", buff);
                output.add(new LongCell(buff[0]));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.value)
                .append(']').toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LongCell)) {
            return false;
        }
        final LongCell other = (LongCell) obj;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return (int) (this.value ^ (this.value >>> 32));
    }

}
