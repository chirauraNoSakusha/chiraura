/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.File;
import java.util.List;

import nippon.kawauso.chiraura.storage.Storage;
import nippon.kawauso.chiraura.storage.Storages;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public class BoardChunkConverterTest {

    private static final File root = new File(System.getProperty("java.io.tmpdir") + File.separator + BoardChunkConverterTest.class.getSimpleName()
            + System.nanoTime());
    private static final int chunkSizeLimit = 1 << 16;
    private static final int directoryBitSize = 8;
    private static final int chunkCacheCapacity = 10;
    private static final int indexCacheCapacity = 20;
    private static final int rangeCacheCapacity = 5;

    /**
     * テスト。
     * @throws Exception 異常
     */
    @Test
    public void testConvert() throws Exception {
        final int nBoard = 100;
        final int nEntry = 100;
        Storage storage = Storages.newInstance(root, chunkSizeLimit, directoryBitSize, chunkCacheCapacity, indexCacheCapacity, rangeCacheCapacity);
        storage.registerChunk(0L, SimpleBoardChunk.class, SimpleBoardChunk.getParser(), SimpleBoardChunk.Id.class, SimpleBoardChunk.Id.getParser());
        storage.registerChunk(2L, OrderingBoardChunk.class, OrderingBoardChunk.getParser(), OrderingBoardChunk.Id.class, OrderingBoardChunk.Id.getParser());

        for (int i = 0; i < nBoard; i++) {
            final SimpleBoardChunk board = new SimpleBoardChunk("" + i);
            for (int j = 0; j < nEntry; j++) {
                board.patch(new SimpleBoardChunk.Entry(i * nEntry + j, i * nEntry + j + 1, "" + (i * nEntry + j + 2), j + 3));
            }
            storage.write(board);
        }
        storage.close();

        storage = Storages.newInstance(root, chunkSizeLimit, directoryBitSize, chunkCacheCapacity, indexCacheCapacity, rangeCacheCapacity);
        BoardChunkConverter.convert(storage);
        storage.close();

        storage = Storages.newInstance(root, chunkSizeLimit, directoryBitSize, chunkCacheCapacity, indexCacheCapacity, rangeCacheCapacity);
        storage.registerChunk(0L, SimpleBoardChunk.class, SimpleBoardChunk.getParser(), SimpleBoardChunk.Id.class, SimpleBoardChunk.Id.getParser());
        storage.registerChunk(2L, OrderingBoardChunk.class, OrderingBoardChunk.getParser(), OrderingBoardChunk.Id.class, OrderingBoardChunk.Id.getParser());
        for (int i = 0; i < nBoard; i++) {
            final OrderingBoardChunk.Id id = new OrderingBoardChunk.Id("" + i);
            final OrderingBoardChunk board = storage.read(id);
            Assert.assertNotNull(board);
            final List<OrderingBoardChunk.Entry> entries = board.getEntries();
            Assert.assertEquals(nEntry, entries.size());
            for (int j = 0; j < nEntry; j++) {
                Assert.assertEquals(entries.get(j).getName(), i * nEntry + j + 1);
                Assert.assertEquals(entries.get(j).getTitle(), "" + (i * nEntry + j + 2));
                Assert.assertEquals(entries.get(j).getNumOfComments(), j + 3);
            }
        }

        storage = Storages.newInstance(root, chunkSizeLimit, directoryBitSize, chunkCacheCapacity, indexCacheCapacity, rangeCacheCapacity);
        BoardChunkConverter.convert(storage);
        storage.close();

        storage = Storages.newInstance(root, chunkSizeLimit, directoryBitSize, chunkCacheCapacity, indexCacheCapacity, rangeCacheCapacity);
        storage.registerChunk(0L, SimpleBoardChunk.class, SimpleBoardChunk.getParser(), SimpleBoardChunk.Id.class, SimpleBoardChunk.Id.getParser());
        storage.registerChunk(2L, OrderingBoardChunk.class, OrderingBoardChunk.getParser(), OrderingBoardChunk.Id.class, OrderingBoardChunk.Id.getParser());
        for (int i = 0; i < nBoard; i++) {
            final OrderingBoardChunk.Id id = new OrderingBoardChunk.Id("" + i);
            final OrderingBoardChunk board = storage.read(id);
            Assert.assertNotNull(board);
            final List<OrderingBoardChunk.Entry> entries = board.getEntries();
            Assert.assertEquals(nEntry, entries.size());
            for (int j = 0; j < nEntry; j++) {
                Assert.assertEquals(entries.get(j).getName(), i * nEntry + j + 1);
                Assert.assertEquals(entries.get(j).getTitle(), "" + (i * nEntry + j + 2));
                Assert.assertEquals(entries.get(j).getNumOfComments(), j + 3);
            }
        }
    }

}
