package nippon.kawauso.chiraura.lib.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * ポート番号の容器。
 * @author chirauraNoSakusha
 */
public final class PortCell implements BytesConvertible {

    private final short value;

    private PortCell(final short value) {
        this.value = value;
    }

    /**
     * ポート番号を与えて作成。
     * @param value ポート番号
     */
    public PortCell(final int value) {
        this(PortFunctions.encodeToShort(value));

        // 事後検査。
        if (!PortFunctions.isValid(value)) {
            throw new IllegalArgumentException("Invalid port number ( " + value + " ) not in [" + PortFunctions.MIN_VALUE + ", " + PortFunctions.MAX_VALUE
                    + "].");
        }
    }

    /**
     * 設定されているポート番号を返す。
     * @return 設定されているポート番号
     */
    public int get() {
        return PortFunctions.decodeFromShort(this.value);
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
     * 復号器を返す。
     * @return 復号器
     */
    public static BytesConvertible.Parser<PortCell> getParser() {
        return new BytesConvertible.Parser<PortCell>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super PortCell> output) throws MyRuleException, IOException {
                final short[] value = new short[1];
                final int size = BytesConversion.fromStream(input, maxByteSize, "cs", value);
                output.add(new PortCell(value[0]));
                return size;
            }
        };
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(PortFunctions.decodeFromShort(this.value))
                .append(']').toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PortCell)) {
            return false;
        }
        final PortCell other = (PortCell) obj;
        return this.value == other.value;
    }

    @Override
    public int hashCode() {
        return this.value;
    }

}
