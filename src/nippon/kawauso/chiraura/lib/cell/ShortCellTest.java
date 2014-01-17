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
public final class ShortCellTest extends BytesConvertibleTest<ShortCell> {

    @Override
    protected ShortCell[] getInstances() {
        final List<ShortCell> list = new ArrayList<>();
        for (final short seed : new short[] { 1, 2, 3, 4, 5, 6, 7 }) {
            list.add(new ShortCell(seed));
        }
        return list.toArray(new ShortCell[0]);
    }

    @Override
    protected BytesConvertible.Parser<ShortCell> getParser() {
        return ShortCell.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

    private final short offset = (short) System.nanoTime();

    @Override
    protected ShortCell getInstance(final int seed) {
        return new ShortCell((short) (BitReversal.getShort((short) seed) + this.offset));
    }

}
