/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.cell.Utf8Cell;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * テスト用のメッセージ。
 * @author chirauraNoSakusha
 */
final class TestMessage implements Message {

    private final int value;
    private final String string;

    TestMessage(final int value, final String string) {
        this.value = value;
        if (string != null) {
            this.string = string;
        } else {
            this.string = "";
        }
    }

    TestMessage(final int value) {
        this(value, null);
    }

    TestMessage(final String string) {
        this(0, string);
    }

    int getValue() {
        return this.value;
    }

    String getString() {
        return this.string;
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("io", this.value, new Utf8Cell(this.string));
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "io", this.value, new Utf8Cell(this.string));
    }

    static BytesConvertible.Parser<TestMessage> getParser() {
        return new BytesConvertible.Parser<TestMessage>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super TestMessage> output) throws MyRuleException, IOException {
                final int[] value = new int[1];
                final List<Utf8Cell> string = new ArrayList<>(1);
                final int size = BytesConversion.fromStream(input, maxByteSize, "io", value, string, Utf8Cell.getParser());
                output.add(new TestMessage(value[0], string.get(0).get()));
                return size;
            }
        };
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.value;
        result = prime * result + ((this.string == null) ? 0 : this.string.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof TestMessage)) {
            return false;
        }
        final TestMessage other = (TestMessage) obj;
        return this.value == other.value && this.string.equals(other.string);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(Integer.toString(this.value))
                .append(", ").append(this.string)
                .append(']').toString();
    }

}
