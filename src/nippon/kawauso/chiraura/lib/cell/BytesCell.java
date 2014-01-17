/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.cell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import nippon.kawauso.chiraura.lib.BytesFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.Hexadecimal;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
public final class BytesCell implements Cell<byte[]>, Comparable<BytesCell> {

    private final byte[] value;

    /**
     * @param value 任意のbyte[]
     */
    public BytesCell(final byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("Null value.");
        }
        this.value = value;
    }

    /**
     * @return 設定されている値
     */
    @Override
    public byte[] get() {
        return this.value;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("ab", this.value);
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "ab", this.value);
    }

    /**
     * @return 復号器
     */
    public static BytesConvertible.Parser<BytesCell> getParser() {
        return new BytesConvertible.Parser<BytesCell>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super BytesCell> output) throws MyRuleException, IOException {
                final byte[][] buff = new byte[1][];
                final int size = BytesConversion.fromStream(input, maxByteSize, "ab", (Object) buff);
                output.add(new BytesCell(buff[0]));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(Hexadecimal.toHexadecimal(this.value))
                .append(']').toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BytesCell)) {
            return false;
        }
        final BytesCell other = (BytesCell) obj;
        return Arrays.equals(this.value, other.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.value);
    }

    @Override
    public int compareTo(final BytesCell o) {
        return BytesFunctions.compare(this.value, o.value);
    }

}
