/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.cell;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BitReversal;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

/**
 * @author chirauraNoSakusha
 */
public final class NumberCellTest extends BytesConvertibleTest<NumberCell> {

    @Override
    protected NumberCell[] getInstances() {
        final List<NumberCell> list = new ArrayList<>();
        for (final long seed : new long[] { 1, 2, 3, 4, 5, 6, 7, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L, Long.MAX_VALUE, Long.MIN_VALUE }) {
            list.add(new NumberCell(seed));
        }
        return list.toArray(new NumberCell[0]);
    }

    @Override
    protected BytesConvertible.Parser<NumberCell> getParser() {
        return NumberCell.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

    private final long offset = System.nanoTime();

    @Override
    protected NumberCell getInstance(final int seed) {
        return new NumberCell(BitReversal.getLong(seed) + this.offset);
    }

}
