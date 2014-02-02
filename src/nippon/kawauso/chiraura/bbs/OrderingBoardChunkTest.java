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
public final class OrderingBoardChunkTest extends BytesConvertibleTest<OrderingBoardChunk> {

    private final long start;

    /**
     * 初期化。
     */
    public OrderingBoardChunkTest() {
        this.start = System.currentTimeMillis();
    }

    @Override
    protected OrderingBoardChunk[] getInstances() {
        final List<OrderingBoardChunk> list = new ArrayList<>();
        {
            final OrderingBoardChunk instance = new OrderingBoardChunk("aaaaa");
            instance.patch(new OrderingBoardChunk.Entry(this.start, 1, this.start / 1_000L, "ああああうえ", 1));
            instance.patch(new OrderingBoardChunk.Entry(this.start + 1, 2, this.start + 1 / 1_000L, "てすと", 1));
            list.add(instance);
        }
        {
            final OrderingBoardChunk instance = new OrderingBoardChunk("bbbbb");
            list.add(instance);
        }
        {
            final OrderingBoardChunk instance = new OrderingBoardChunk("ccccc");
            instance.patch(new OrderingBoardChunk.Entry(this.start + 5, 3, this.start + 5 / 1_000L, "ああああうえ", 1));
            list.add(instance);
        }
        return list.toArray(new OrderingBoardChunk[0]);
    }

    @Override
    protected OrderingBoardChunk getInstance(final int seed) {
        final OrderingBoardChunk instance = new OrderingBoardChunk(Integer.toString(seed));
        for (int i = 0; i < 5; i++) {
            instance.patch(new OrderingBoardChunk.Entry(this.start + seed + i, seed, this.start / 1_000L + seed + i, Integer.toString(seed + i), i));
        }
        return instance;
    }

    @Override
    protected BytesConvertible.Parser<OrderingBoardChunk> getParser() {
        return OrderingBoardChunk.getParser();
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
        final OrderingBoardChunk instance = new OrderingBoardChunk(board);
        final List<OrderingBoardChunk.Entry> pool = new ArrayList<>();
        for (int i = 0;; i++) {
            final int beforeSize = instance.byteSize();
            final OrderingBoardChunk.Entry entry = new OrderingBoardChunk.Entry(this.start + i, i, this.start / 1_000L + i, Integer.toString(i), 1);
            pool.add(entry);
            Assert.assertTrue(instance.patch(entry));
            final int afterSize = instance.byteSize();
            if (afterSize < beforeSize + entry.byteSize()) {
                // System.out.println(beforeSize + " " + afterSize);
                break;
            }
        }
        final List<OrderingBoardChunk.Entry> entries = instance.getEntries();
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
