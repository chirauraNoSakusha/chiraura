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
public final class LongCellTest extends BytesConvertibleTest<LongCell> {

    @Override
    protected LongCell[] getInstances() {
        final List<LongCell> list = new ArrayList<>();
        for (final long seed : new long[] { 1, 2, 3, 4, 5, 6, 7 }) {
            list.add(new LongCell(seed));
        }
        return list.toArray(new LongCell[0]);
    }

    @Override
    protected BytesConvertible.Parser<LongCell> getParser() {
        return LongCell.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

    private final long offset = System.nanoTime();

    @Override
    protected LongCell getInstance(final int seed) {
        return new LongCell(BitReversal.getLong(seed) + this.offset);
    }

}
