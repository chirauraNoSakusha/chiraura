/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.cell;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
public final class Utf8Cell extends StringCell {

    private static final Charset CHARSET = Charset.forName("UTF-8");

    /**
     * @param value 中身の文字列
     */
    public Utf8Cell(final String value) {
        super(value);
    }

    private Utf8Cell(final Proxy proxy) {
        super(proxy);
    }

    @Override
    protected Charset getCharset() {
        return CHARSET;
    }

    /**
     * @return 復号器
     */
    public static BytesConvertible.Parser<Utf8Cell> getParser() {
        return new BytesConvertible.Parser<Utf8Cell>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super Utf8Cell> output) throws MyRuleException, IOException {
                final List<Proxy> proxy = new ArrayList<>(1);
                final int size = Proxy.getParser(CHARSET).fromStream(input, maxByteSize, proxy);
                output.add(new Utf8Cell(proxy.get(0)));
                return size;
            }
        };
    }

}
