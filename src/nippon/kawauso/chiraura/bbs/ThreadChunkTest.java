/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class ThreadChunkTest extends BytesConvertibleTest<ThreadChunk> {

    private final long start;

    /**
     * 初期化。
     */
    public ThreadChunkTest() {
        this.start = System.currentTimeMillis();
    }

    @Override
    protected ThreadChunk[] getInstances() {
        final List<ThreadChunk> list = new ArrayList<>();
        {
            final ThreadChunk instance = new ThreadChunk("aaaaa", this.start / Duration.SECOND, "あああスレ", "名無し", "age", this.start, 0, "あああああああああ");
            instance.patch(ThreadChunk.Entry.newInstance("俺様", "sage", this.start + 1, 1, PostFunctions.wrapMessage(">>1 死ね")));
            instance.patch(ThreadChunk.Entry.newInstance("名無し", "age", this.start + 2, 2, PostFunctions.wrapMessage(">>2 同意")));
            list.add(instance);
        }
        {
            final ThreadChunk instance = new ThreadChunk("bbbbb", this.start / Duration.SECOND + 1, "くそスレ", "名無し", "age", this.start + 3, 3, "自分で言うとか謙虚");
            list.add(instance);
        }
        {
            final ThreadChunk instance = new ThreadChunk("cccccc", this.start / Duration.SECOND + 2, "人はなぜ生きるのか？", "我はメシア", "age", this.start + 4, 4, "我に従え");
            instance.patch(ThreadChunk.Entry.newInstance("通りすがり", "age", this.start + 5, 5, "神"));
            list.add(instance);
        }
        return list.toArray(new ThreadChunk[0]);
    }

    @Override
    protected ThreadChunk getInstance(final int seed) {
        String label = Integer.toString(seed);
        final ThreadChunk instance = new ThreadChunk(label, this.start / Duration.SECOND + seed, label, label, label, this.start + seed, seed, label);
        for (int i = 1; i < 5; i++) {
            label = Integer.toString(seed + i);
            instance.patch(ThreadChunk.Entry.newInstance(label, label, this.start + seed + i, seed + i, label));
        }
        return instance;
    }

    @Override
    protected BytesConvertible.Parser<ThreadChunk> getParser() {
        return ThreadChunk.getParser();
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
        String label = Long.toString(this.start);
        final ThreadChunk instance = new ThreadChunk(board, this.start / Duration.SECOND, label, label, label, this.start, this.start, label);
        final List<ThreadChunk.Entry> pool = new ArrayList<>();
        for (int i = 0;; i++) {
            final int beforeSize = instance.byteSize();
            label = Long.toString(this.start + i);
            final ThreadChunk.Entry entry = ThreadChunk.Entry.newInstance(label, label, this.start + i, i, label);
            pool.add(entry);
            final boolean patched = instance.patch(entry);
            final int afterSize = instance.byteSize();
            if (afterSize < beforeSize + entry.byteSize()) {
                Assert.assertFalse(patched);
                // System.out.println(beforeSize + " " + afterSize);
                break;
            }
            Assert.assertTrue(patched);
        }
        final List<ThreadChunk.Entry> entries = instance.getEntries();
        Assert.assertEquals(pool.size() - 1, entries.size());
        for (int i = 0; i < entries.size(); i++) {
            Assert.assertEquals(pool.get(i), entries.get(i));
        }

        // System.out.println(instance.toNetworkString());
        // System.out.println(instance.getNumOfComments());
    }

}
