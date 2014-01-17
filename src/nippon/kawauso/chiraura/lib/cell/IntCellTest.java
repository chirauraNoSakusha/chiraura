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
public final class IntCellTest extends BytesConvertibleTest<IntCell> {

    @Override
    protected IntCell[] getInstances() {
        final List<IntCell> list = new ArrayList<>();
        for (final int seed : new int[] { 1, 2, 3, 4, 5, 6, 7 }) {
            list.add(new IntCell(seed));
        }
        return list.toArray(new IntCell[0]);
    }

    @Override
    protected BytesConvertible.Parser<IntCell> getParser() {
        return IntCell.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

    private static final int offset = (int) System.nanoTime();

    @Override
    protected IntCell getInstance(final int seed) {
        return new IntCell(BitReversal.getInt(seed) + offset);
    }

}
