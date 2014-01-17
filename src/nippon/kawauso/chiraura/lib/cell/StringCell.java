/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.cell;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
abstract class StringCell implements Cell<String>, Comparable<StringCell> {

    private final String value;

    protected abstract Charset getCharset();

    /**
     * @param value 任意の文字列
     */
    public StringCell(final String value) {
        if (value == null) {
            throw new IllegalArgumentException("Null value.");
        }
        this.value = value;
    }

    protected StringCell(final Proxy proxy) {
        this(proxy.value);
    }

    /**
     * @return 設定されている文字列
     */
    @Override
    public String get() {
        return this.value;
    }

    public byte[] getIndependentBytes() {
        return this.value.getBytes(getCharset());
    }

    @Override
    public int byteSize() {
        return BytesConversion.byteSize("ab", this.value.getBytes(getCharset()));
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return BytesConversion.toStream(output, "ab", this.value.getBytes(getCharset()));
    }

    protected static final class Proxy {

        protected final String value;

        protected Proxy(final String value) {
            this.value = value;
        }

        protected static BytesConvertible.Parser<Proxy> getParser(final Charset charset) {
            return new ParserImpl(charset);
        }

        protected static final class ParserImpl implements BytesConvertible.Parser<Proxy> {
            private final Charset charset;

            public ParserImpl(final Charset charset) {
                this.charset = charset;
            }

            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super Proxy> output) throws MyRuleException, IOException {
                final byte[][] buff = new byte[1][];
                final int size = BytesConversion.fromStream(input, maxByteSize, "ab", (Object) buff);
                output.add(new Proxy(new String(buff[0], this.charset)));
                return size;
            }
        }

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
        if (!(obj instanceof StringCell)) {
            return false;
        }
        final StringCell other = (StringCell) obj;
        if (!this.value.equals(other.value)) {
            return false;
        }
        return getCharset().equals(other.getCharset());
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public int compareTo(final StringCell o) {
        final int cmp = this.value.compareTo(o.value);
        if (cmp != 0) {
            return cmp;
        }
        return getCharset().compareTo(o.getCharset());
    }

}
