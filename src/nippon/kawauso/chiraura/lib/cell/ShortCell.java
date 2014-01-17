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
public final class ShortCell implements BytesConvertible {

    private final short value;

    /**
     * @param value 任意のshort値
     */
    public ShortCell(final short value) {
        this.value = value;
    }

    /**
     * @return 設定されているshort値
     */
    public short get() {
        return this.value;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("cs", this.value);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "cs", this.value);
    }

    /**
     * @return 復号器
     */
    public static BytesConvertible.Parser<ShortCell> getParser() {
        return new BytesConvertible.Parser<ShortCell>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super ShortCell> output) throws MyRuleException, IOException {
                final short[] value = new short[1];
                final int size = BytesConversion.fromStream(input, maxByteSize, "cs", value);
                output.add(new ShortCell(value[0]));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(Short.toString(this.value))
                .append(']').toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ShortCell)) {
            return false;
        }
        final ShortCell other = (ShortCell) obj;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return this.value;
    }

}
