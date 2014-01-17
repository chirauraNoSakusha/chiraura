/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.cell;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;
import nippon.kawauso.chiraura.lib.test.CharTable;

/**
 * @author chirauraNoSakusha
 */
public final class Utf8CellTest extends BytesConvertibleTest<Utf8Cell> {

    @Override
    protected Utf8Cell[] getInstances() {
        final List<Utf8Cell> list = new ArrayList<>();
        for (final String seed : new String[] { "いろは", "に", "ほへとちり", "ぬるを", "わかよたれそつねならむ", "うゐのおくやま", "けふこ", "えてあさきゆめみしゑいもせす" }) {
            list.add(new Utf8Cell(seed));
        }
        return list.toArray(new Utf8Cell[0]);
    }

    @Override
    protected BytesConvertible.Parser<Utf8Cell> getParser() {
        return Utf8Cell.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

    @Override
    protected Utf8Cell getInstance(final int seed) {
        final int length = 100;
        final int first = seed % CharTable.TABLE.length();
        final int last = (seed + length) % CharTable.TABLE.length();
        final String string;
        if (first < last) {
            string = CharTable.TABLE.substring(first, last);
        } else {
            string = CharTable.TABLE.substring(first) + CharTable.TABLE.substring(0, last);
        }
        return new Utf8Cell(string);
    }

}
