/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.cell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

/**
 * @author chirauraNoSakusha
 */
public final class BytesCellTest extends BytesConvertibleTest<BytesCell> {

    private static int MAX_SIZE = 4096;

    @Override
    protected BytesCell[] getInstances() {
        final List<BytesCell> list = new ArrayList<>();
        for (final byte[] seed : new byte[][] { new byte[] { 1, 2, 3, 4, 5 }, new byte[] { 6, 7, 8 }, new byte[] { 9 }, new byte[] {}, new byte[] { 10, 11 } }) {
            list.add(new BytesCell(seed));
        }
        return list.toArray(new BytesCell[0]);
    }

    @Override
    protected BytesConvertible.Parser<BytesCell> getParser() {
        return BytesCell.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

    @Override
    protected BytesCell getInstance(final int seed) {
        final byte[] buff = new byte[seed % MAX_SIZE];
        Arrays.fill(buff, (byte) seed);
        return new BytesCell(buff);
    }

}
