/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.connection;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

/**
 * @author chirauraNoSakusha
 */
public final class PortCellTest extends BytesConvertibleTest<PortCell> {

    @Override
    protected PortCell[] getInstances() {
        final List<PortCell> list = new ArrayList<>();
        for (final short seed : new short[] { 1, 2, 3, 4, 5, 6, 7 }) {
            list.add(new PortCell(seed));
        }
        return list.toArray(new PortCell[0]);
    }

    @Override
    protected BytesConvertible.Parser<PortCell> getParser() {
        return PortCell.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

    @Override
    protected PortCell getInstance(final int seed) {
        return new PortCell(seed % (1 << 16));
    }

}
