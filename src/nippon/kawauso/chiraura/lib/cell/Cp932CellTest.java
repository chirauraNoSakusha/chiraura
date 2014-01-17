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
public final class Cp932CellTest extends BytesConvertibleTest<Cp932Cell> {

    @Override
    protected Cp932Cell[] getInstances() {
        final List<Cp932Cell> list = new ArrayList<>();
        for (final String seed : new String[] { "いろは", "に", "ほへとちり", "ぬるを", "わかよたれそつねならむ", "うゐのおくやま", "けふこ", "えてあさきゆめみしゑいもせす" }) {
            list.add(new Cp932Cell(seed));
        }
        return list.toArray(new Cp932Cell[0]);
    }

    @Override
    protected BytesConvertible.Parser<Cp932Cell> getParser() {
        return Cp932Cell.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

    @Override
    protected Cp932Cell getInstance(final int seed) {
        final int length = 100;
        final int first = seed % CharTable.TABLE.length();
        final int last = (seed + length) % CharTable.TABLE.length();
        final String string;
        if (first < last) {
            string = CharTable.TABLE.substring(first, last);
        } else {
            string = CharTable.TABLE.substring(first) + CharTable.TABLE.substring(0, last);
        }
        return new Cp932Cell(string);
    }

}
