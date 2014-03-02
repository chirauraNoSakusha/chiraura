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
public final class IntCell implements BytesConvertible {

    private final int value;

    /**
     * @param value 任意のint値
     */
    public IntCell(final int value) {
        this.value = value;
    }

    /**
     * @return 設定されている値
     */
    public int get() {
        return this.value;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("ci", this.value);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "ci", this.value);
    }

    /**
     * @return 復号器
     */
    public static BytesConvertible.Parser<IntCell> getParser() {
        return new BytesConvertible.Parser<IntCell>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super IntCell> output) throws MyRuleException, IOException {
                final int[] value = new int[1];
                final int size = BytesConversion.fromStream(input, maxByteSize, "ci", value);
                output.add(new IntCell(value[0]));
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
        if (!(obj instanceof IntCell)) {
            return false;
        }
        final IntCell other = (IntCell) obj;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return this.value;
    }

}
