package nippon.kawauso.chiraura.storage;

import nippon.kawauso.chiraura.lib.math.MathFunctions;
import nippon.kawauso.chiraura.lib.test.TestFunctions;

import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class MemoryStorageTest {

    private final String prefix;

    /**
     * 初期化。
     */
    public MemoryStorageTest() {
        this.prefix = this.getClass().getName() + ":";
        TestFunctions.testLogging(this.getClass().getName());
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testMinimum() throws Exception {
        StorageTest.testMinimum(new MemoryStorage());
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testConcurrencyPerformanceByConstantChunk() throws Exception {
        final int numOfLoops = 1_000;
        final int numOfChunks = 100;
        final int numOfProcesses = 1_000;
        final int chunkSize = 2 * (int) ((MathFunctions.log2(numOfChunks) + Byte.SIZE - 1) / Byte.SIZE);
        StorageTest.testConcurrencyPerformanceByConstantChunk(new MemoryStorage(), numOfLoops, numOfChunks, numOfProcesses, chunkSize, this.prefix);
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testConcurrencyByVariableChunk() throws Exception {
        final int numOfLoops = 1_000;
        final int numOfChunks = 100;
        final int numOfProcesses = 1_000;
        StorageTest.testConcurrencyByVariableChunk(new MemoryStorage(), numOfLoops, numOfChunks, numOfProcesses, this.prefix);
    }

}
