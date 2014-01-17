/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class BoardChunkTest extends BytesConvertibleTest<BoardChunk> {

    private final long start;

    /**
     * 初期化。
     */
    public BoardChunkTest() {
        this.start = System.currentTimeMillis();
    }

    @Override
    protected BoardChunk[] getInstances() {
        final List<BoardChunk> list = new ArrayList<>();
        {
            final BoardChunk instance = new BoardChunk("aaaaa");
            instance.patch(new BoardChunk.Entry(this.start, this.start / 1_000L, "ああああうえ", 1));
            instance.patch(new BoardChunk.Entry(this.start + 1, this.start + 1 / 1_000L, "てすと", 1));
            list.add(instance);
        }
        {
            final BoardChunk instance = new BoardChunk("bbbbb");
            list.add(instance);
        }
        {
            final BoardChunk instance = new BoardChunk("ccccc");
            instance.patch(new BoardChunk.Entry(this.start + 5, this.start + 5 / 1_000L, "ああああうえ", 1));
            list.add(instance);
        }
        return list.toArray(new BoardChunk[0]);
    }

    @Override
    protected BoardChunk getInstance(final int seed) {
        final BoardChunk instance = new BoardChunk(Integer.toString(seed));
        for (int i = 0; i < 5; i++) {
            instance.patch(new BoardChunk.Entry(this.start + seed + i, this.start / 1_000L + seed + i, Integer.toString(seed + i), i));
        }
        return instance;
    }

    @Override
    protected BytesConvertible.Parser<BoardChunk> getParser() {
        return BoardChunk.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

    /**
     * 限界突破の検査。
     */
    @Test
    public void testLimit() {
        final String board = "test";
        final BoardChunk instance = new BoardChunk(board);
        final List<BoardChunk.Entry> pool = new ArrayList<>();
        for (int i = 0;; i++) {
            final int beforeSize = instance.byteSize();
            final BoardChunk.Entry entry = new BoardChunk.Entry(this.start + i, this.start / 1_000L + i, Integer.toString(i), 1);
            pool.add(entry);
            Assert.assertTrue(instance.patch(entry));
            final int afterSize = instance.byteSize();
            if (afterSize < beforeSize + entry.byteSize()) {
                // System.out.println(beforeSize + " " + afterSize);
                break;
            }
        }
        final List<BoardChunk.Entry> entries = instance.getEntries();
        Assert.assertEquals(pool.get(pool.size() - 1), entries.get(entries.size() - 1));
        Assert.assertNotEquals(pool.get(0), entries.get(0));
        int offset = 1;
        for (; !entries.get(0).equals(pool.get(offset)); offset++) {
        }
        for (int i = 0; i < entries.size(); i++) {
            Assert.assertEquals(pool.get(offset + i), entries.get(i));
        }
    }

}
