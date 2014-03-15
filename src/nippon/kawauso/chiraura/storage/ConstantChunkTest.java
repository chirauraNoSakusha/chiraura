package nippon.kawauso.chiraura.storage;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;

/**
 * @author chirauraNoSakusha
 */
public final class ConstantChunkTest extends BytesConvertibleTest<ConstantChunk> {

    @Override
    protected ConstantChunk[] getInstances() {
        final List<ConstantChunk> list = new ArrayList<>();
        list.add(new ConstantChunk(0, "あほ".getBytes()));
        list.add(new ConstantChunk(1, "ばか".getBytes()));
        list.add(new ConstantChunk(2, "まぬけ".getBytes()));
        return list.toArray(new ConstantChunk[0]);
    }

    /**
     * 試験用に識別子を作成する。
     * @param seed 種
     * @return 試験用識別子
     */
    public static ConstantChunk.Id newId(final int seed) {
        return new ConstantChunk.Id(HashValue.calculateFromBytes(NumberBytesConversion.toBytes(seed)));
    }

    /**
     * 試験用に作成する。
     * @param seed 種
     * @return 試験用インスタンス
     */
    public static ConstantChunk newInstance(final int seed) {
        return new ConstantChunk(seed, NumberBytesConversion.toBytes(seed));
    }

    @Override
    protected ConstantChunk getInstance(final int seed) {
        return newInstance(seed);
    }

    @Override
    protected BytesConvertible.Parser<ConstantChunk> getParser() {
        return ConstantChunk.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
