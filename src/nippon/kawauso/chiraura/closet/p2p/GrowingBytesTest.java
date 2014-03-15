package nippon.kawauso.chiraura.closet.p2p;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

/**
 * @author chirauraNoSakusha
 */
public final class GrowingBytesTest extends BytesConvertibleTest<GrowingBytes> {

    @Override
    protected GrowingBytes[] getInstances() {
        final List<GrowingBytes> list = new ArrayList<>();
        list.add(new GrowingBytes("あほ"));
        list.add(new GrowingBytes("ばか"));
        list.get(list.size() - 1).patch(new GrowingBytes.Entry(new byte[] { 12, 34, 56 }));
        return list.toArray(new GrowingBytes[0]);
    }

    static GrowingBytes.Id newId(final int seed) {
        return new GrowingBytes.Id(Integer.toString(seed));
    }

    static GrowingBytes newInstance(final int seed) {
        final GrowingBytes instance = new GrowingBytes(Integer.toString(seed), seed);
        for (int i = 1; i <= 10; i++) {
            instance.patch(GrowingBytesEntryTest.newDiff(seed + i));
        }
        return instance;
    }

    @Override
    protected GrowingBytes getInstance(final int seed) {
        return newInstance(seed);
    }

    @Override
    protected BytesConvertible.Parser<GrowingBytes> getParser() {
        return GrowingBytes.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
