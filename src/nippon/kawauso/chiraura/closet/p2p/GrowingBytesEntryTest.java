/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;

/**
 * @author chirauraNoSakusha
 */
public final class GrowingBytesEntryTest extends BytesConvertibleTest<GrowingBytes.Entry> {

    @Override
    protected GrowingBytes.Entry[] getInstances() {
        final List<GrowingBytes.Entry> list = new ArrayList<>();
        list.add(new GrowingBytes.Entry(new byte[] { 12, 34, 56 }));
        list.add(new GrowingBytes.Entry(new byte[] { 12, 34, 56 }));
        list.add(new GrowingBytes.Entry(new byte[] { 12 }));
        return list.toArray(new GrowingBytes.Entry[0]);
    }

    static GrowingBytes.Entry newDiff(final int seed) {
        return new GrowingBytes.Entry(seed, NumberBytesConversion.toBytes(seed));
    }

    @Override
    protected GrowingBytes.Entry getInstance(final int seed) {
        return newDiff(seed);
    }

    @Override
    protected BytesConvertible.Parser<GrowingBytes.Entry> getParser() {
        return GrowingBytes.Entry.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
