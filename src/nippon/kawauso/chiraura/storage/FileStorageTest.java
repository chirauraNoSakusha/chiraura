package nippon.kawauso.chiraura.storage;

import java.io.File;

import nippon.kawauso.chiraura.lib.math.MathFunctions;
import nippon.kawauso.chiraura.lib.test.TestFunctions;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class FileStorageTest {

    private final File root;
    private final int chunkSizeLimit;
    private final int directoryBitSize;

    /**
     * 初期化。
     */
    public FileStorageTest() {
        this.root = new File(System.getProperty("java.io.tmpdir") + File.separator + FileStorageTest.class.getSimpleName() + File.separator + System.nanoTime());

        this.chunkSizeLimit = 1024 * 4;
        this.directoryBitSize = 7;
        TestFunctions.testLogging(this.getClass().getSimpleName());
        // TestFunctions.testLogging(Level.ALL, Level.OFF);
    }

    private void checkTrash() {
        final File trash = new File(this.root, "%%trash%%");
        final File[] files = trash.listFiles();
        Assert.assertArrayEquals(new File[0], files);
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testMinimum() throws Exception {
        // LoggingFunctions.startDebugLogging();
        StorageTest.testMinimum(new FileStorage(this.root, this.chunkSizeLimit, this.directoryBitSize));
        checkTrash();
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testRandom() throws Exception {
        final int numOfLoops = 100_000;
        final int numOfChunks = 100;
        StorageTest.testRandom(new MemoryStorage(), new FileStorage(this.root, this.chunkSizeLimit, this.directoryBitSize), numOfLoops, numOfChunks);
        checkTrash();
    }

    private static final String PREFIX = FileStorage.class.getName() + ":";

    /**
     * @throws Exception 異常
     */
    @Test
    public void testConcurrencyPerformanceByConstantChunk() throws Exception {
        final int numOfLoops = 100;
        final int numOfChunks = 100;
        final int numOfProcesses = 1_000;
        final int chunkSize = 2 * (int) ((MathFunctions.log2(numOfChunks) + Byte.SIZE - 1) / Byte.SIZE);
        // LoggingFunctions.startLogging(Level.SEVERE);
        StorageTest.testConcurrencyPerformanceByConstantChunk(new FileStorage(this.root, this.chunkSizeLimit, this.directoryBitSize), numOfLoops, numOfChunks,
                numOfProcesses, chunkSize, PREFIX);
        checkTrash();
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testConcurrencyByVariableChunk() throws Exception {
        final int numOfLoops = 100;
        final int numOfChunks = 100;
        final int numOfProcesses = 1_000;
        StorageTest.testConcurrencyByVariableChunk(new FileStorage(this.root, this.chunkSizeLimit, this.directoryBitSize), numOfLoops, numOfChunks,
                numOfProcesses, PREFIX);
        checkTrash();
    }

    /**
     * ファイルを返す。
     * @param root トップディレクトリ
     * @param directoryBitSize ディレクトリ用のビット数
     * @param id データ片の識別子
     * @param type データ片の識別子の型
     * @return データ片を保存するファイル
     */
    public static File getFile(final File root, final int directoryBitSize, final Chunk.Id<?> id, final long type) {
        return new File(root, FileStorage.getBase(directoryBitSize, id, type));
    }

}
