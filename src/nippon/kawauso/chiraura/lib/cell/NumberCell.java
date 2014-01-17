/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.cell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
public final class NumberCell implements BytesConvertible {

    private final long value;

    /**
     * @param value 任意のlong値
     */
    public NumberCell(final long value) {
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
        return NumberBytesConversion.byteSize(this.value);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return NumberBytesConversion.toStream(this.value, output);
    }

    /**
     * @return 復号器
     */
    public static BytesConvertible.Parser<NumberCell> getParser() {
        return new BytesConvertible.Parser<NumberCell>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super NumberCell> output) throws MyRuleException, IOException {
                final long[] value = new long[1];
                final int size = NumberBytesConversion.fromStream(input, maxByteSize, value);
                output.add(new NumberCell(value[0]));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(Long.toString(this.value))
                .append(']').toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof NumberCell)) {
            return false;
        }
        final NumberCell other = (NumberCell) obj;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return (int) (this.value ^ (this.value >>> 32));
    }

}
