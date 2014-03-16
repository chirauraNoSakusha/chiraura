package nippon.kawauso.chiraura.storage;

import java.io.File;

import nippon.kawauso.chiraura.lib.math.MathFunctions;
import nippon.kawauso.chiraura.lib.test.TestFunctions;

import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class RangeIndexingStorageTest {

    private final File root;
    private final int chunkSizeLimit;
    private final int directoryBitSize;
    private final String prefix;

    /**
     * 初期化。
     */
    public RangeIndexingStorageTest() {
        this.root = new File(System.getProperty("java.io.tmpdir") + File.separator + FileStorageTest.class.getSimpleName() + File.separator + System.nanoTime());
        this.chunkSizeLimit = 1024 * 1024 + 1024;
        this.directoryBitSize = 6;
        this.prefix = RangeIndexingStorage.class.getName() + " on " + FileStorage.class.getSimpleName();
        TestFunctions.testLogging(this.getClass().getSimpleName());
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testMinimum() throws Exception {
        StorageTest.testMinimum(new RangeIndexingStorage(new MemoryStorage(), 1));
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testRandom() throws Exception {
        final int numOfLoops = 100_000;
        final int numOfChunks = 100;
        final Storage instance = new RangeIndexingStorage(new FileStorage(this.root, this.chunkSizeLimit, this.directoryBitSize), numOfChunks / 5);
        StorageTest.testRandom(new MemoryStorage(), instance, numOfLoops, numOfChunks);
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
        final Storage instance = new RangeIndexingStorage(new FileStorage(this.root, this.chunkSizeLimit, this.directoryBitSize), numOfChunks / 5);
        StorageTest.testConcurrencyPerformanceByConstantChunk(instance, numOfLoops, numOfChunks, numOfProcesses, chunkSize, this.prefix);
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testConcurrencyByVariableChunk() throws Exception {
        final int numOfLoops = 100;
        final int numOfChunks = 100;
        final int numOfProcesses = 1_000;
        final Storage instance = new RangeIndexingStorage(new FileStorage(this.root, this.chunkSizeLimit, this.directoryBitSize), numOfChunks / 5);
        StorageTest.testConcurrencyByVariableChunk(instance, numOfLoops, numOfChunks, numOfProcesses, this.prefix);
    }

}