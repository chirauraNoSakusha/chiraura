package nippon.kawauso.chiraura.storage;

import java.io.File;

import nippon.kawauso.chiraura.lib.math.MathFunctions;
import nippon.kawauso.chiraura.lib.test.TestFunctions;

import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class IndexingStorageTest {

    private final File root;
    private final int chunkSizeLimit;
    private final int directoryBitSize;
    private final double factor;
    private final String prefix;

    /**
     * 初期化。
     */
    public IndexingStorageTest() {
        this.root = new File(System.getProperty("java.io.tmpdir") + File.separator + FileStorageTest.class.getName() + File.separator + System.nanoTime());
        this.chunkSizeLimit = 1024 * 1024 + 1024;
        this.directoryBitSize = 6;
        this.factor = 0.5;
        this.prefix = IndexingStorage.class.getName() + " on " + FileStorage.class.getSimpleName() + String.format(" %.2f", this.factor);
        TestFunctions.testLogging(this.getClass().getName());
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testMinimum() throws Exception {
        StorageTest.testMinimum(new IndexingStorage(new MemoryStorage(), 1));
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testRandom() throws Exception {
        final int numOfLoops = 100_000;
        final int numOfChunks = 100;
        StorageTest.testRandom(new MemoryStorage(), new IndexingStorage(new MemoryStorage(), (int) (this.factor * numOfChunks)), numOfLoops, numOfChunks);
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testConcurrencyPerformanceByConstantChunk() throws Exception {
        final int numOfLoops = 100;
        final int numOfChunks = 100;
        final int numOfProcesses = 1_000;
        final int chunkSize = 2 * (int) ((MathFunctions.log2(numOfChunks) + Byte.SIZE - 1) / Byte.SIZE);
        StorageTest.testConcurrencyPerformanceByConstantChunk(new IndexingStorage(new FileStorage(this.root, this.chunkSizeLimit, this.directoryBitSize),
                (int) (this.factor * numOfChunks)), numOfLoops, numOfChunks, numOfProcesses, chunkSize, this.prefix);
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testConcurrencyByVariableChunk() throws Exception {
        final int numOfLoops = 100;
        final int numOfChunks = 100;
        final int numOfProcesses = 1_000;
        StorageTest.testConcurrencyByVariableChunk(new IndexingStorage(new FileStorage(this.root, this.chunkSizeLimit, this.directoryBitSize),
                (int) (this.factor * numOfChunks)),
                numOfLoops, numOfChunks, numOfProcesses, this.prefix);
    }

}
