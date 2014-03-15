package nippon.kawauso.chiraura.storage;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;

/**
 * @author chirauraNoSakusha
 */
public final class VariableChunkTest extends BytesConvertibleTest<VariableChunk> {

    @Override
    protected VariableChunk[] getInstances() {
        final List<VariableChunk> list = new ArrayList<>();
        list.add(new VariableChunk("あほ", 0, "あほ".getBytes()));
        list.add(new VariableChunk("ばか", 1, "ばか".getBytes()));
        list.add(new VariableChunk("まぬけ", 2, "まぬけ".getBytes()));
        return list.toArray(new VariableChunk[0]);
    }

    /**
     * 試験用に識別子を作成する。
     * @param seed 種
     * @return 試験用識別子
     */
    public static VariableChunk.Id newId(final int seed) {
        return new VariableChunk.Id(Integer.toString(seed));
    }

    /**
     * 試験用に作成する。
     * @param seed 種
     * @return 試験用インスタンス
     */
    public static VariableChunk newInstance(final int seed) {
        return new VariableChunk(Integer.toString(seed), seed, NumberBytesConversion.toBytes(seed));
    }

    @Override
    protected VariableChunk getInstance(final int seed) {
        return newInstance(seed);
    }

    @Override
    protected BytesConvertible.Parser<VariableChunk> getParser() {
        return VariableChunk.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

}
