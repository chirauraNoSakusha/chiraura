package nippon.kawauso.chiraura.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;
import nippon.kawauso.chiraura.lib.math.MathFunctions;
import nippon.kawauso.chiraura.lib.test.BiasedRandom;
import nippon.kawauso.chiraura.lib.test.TestFunctions;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class WriteCachingStorageTest {

    private final File root;
    private final int chunkSizeLimit;
    private final int directoryBitSize;
    private final double factor;
    private final String prefix;

    /**
     * 初期化。
     */
    public WriteCachingStorageTest() {
        this.root = new File(System.getProperty("java.io.tmpdir") + File.separator + FileStorageTest.class.getSimpleName() + System.nanoTime());
        this.chunkSizeLimit = 1024 * 1024 + 1024;
        this.directoryBitSize = 6;
        this.factor = 0.1;
        this.prefix = WriteCachingStorage.class.getName() + " on " + FileStorage.class.getSimpleName() + String.format(" %.2f", this.factor);
        TestFunctions.testLogging(this.getClass().getName());
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testMinimum() throws Exception {
        StorageTest.testMinimum(new WriteCachingStorage(new MemoryStorage(), 1));
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testRandom() throws Exception {
        final int numOfLoops = 100_000;
        final int numOfChunks = 100;
        final Storage instance = new WriteCachingStorage(new FileStorage(this.root, this.chunkSizeLimit, this.directoryBitSize),
                (int) (this.factor * numOfChunks));
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
        final Storage instance = new WriteCachingStorage(new FileStorage(this.root, this.chunkSizeLimit, this.directoryBitSize),
                (int) (this.factor * numOfChunks));
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
        final Storage instance = new WriteCachingStorage(new FileStorage(this.root, this.chunkSizeLimit, this.directoryBitSize),
                (int) (this.factor * numOfChunks));
        StorageTest.testConcurrencyByVariableChunk(instance, numOfLoops, numOfChunks, numOfProcesses, this.prefix);
    }

    /**
     * キャッシュが最終的にちゃんと反映されるかどうかの検査。
     * @throws Exception 異常
     */
    @Test
    public void testClear() throws Exception {
        final int numOfLoops = 1_000;
        final int numOfChunks = 100;

        final long seed = System.nanoTime();

        // データ片の準備。
        final List<VariableChunk.Id> chunkIds = new ArrayList<>(numOfChunks);
        for (int i = 0; i < numOfChunks; i++) {
            chunkIds.add(new VariableChunk.Id(Integer.toString(i)));
        }

        final Random uniform = new Random(seed);
        final BiasedRandom bias = new BiasedRandom(uniform);
        final File root1 = new File(this.root.getPath() + "a");
        Assert.assertTrue(root1.mkdir());
        final File root2 = new File(this.root.getPath() + "b");
        Assert.assertTrue(root2.mkdir());

        try (final Storage instance1 = new FileStorage(root1, this.chunkSizeLimit, this.directoryBitSize);
                final Storage instance2 = new WriteCachingStorage(new FileStorage(root2, this.chunkSizeLimit, this.directoryBitSize),
                        (int) (this.factor * numOfChunks))) {
            instance1.registerChunk(0, VariableChunk.class, VariableChunk.getParser(), VariableChunk.Id.class, VariableChunk.Id.getParser());
            instance2.registerChunk(0, VariableChunk.class, VariableChunk.getParser(), VariableChunk.Id.class, VariableChunk.Id.getParser());
            for (int i = 0; i < numOfLoops; i++) {
                final VariableChunk.Id target = chunkIds.get((int) (bias.next() * chunkIds.size()));

                final double flag = uniform.nextDouble();
                if (flag < 1.0 / 4.0) {
                    Assert.assertEquals(instance1.contains(target), instance2.contains(target));
                } else if (flag < 2.0 / 4.0) {
                    final Chunk chunk = new VariableChunk(target.getName(), i, NumberBytesConversion.toBytes(0));
                    instance1.write(chunk);
                    instance2.write(chunk);
                } else if (flag < 3.0 / 4.0) {
                    Assert.assertEquals(instance1.read(target), instance2.read(target));
                } else {
                    instance1.lock(target);
                    instance2.lock(target);
                    try {
                        final Chunk chunk1 = instance1.read(target);
                        final Chunk chunk2 = instance2.read(target);
                        Assert.assertEquals(chunk1, chunk2);
                        if (chunk1 != null) {
                            Assert.assertTrue(chunk1 instanceof VariableChunk);
                            Assert.assertTrue(chunk2 instanceof VariableChunk);
                            int value;
                            value = NumberBytesConversion.intFromBytes(((VariableChunk) chunk1).getBody());
                            instance1.write(new VariableChunk(target.getName(), i, NumberBytesConversion.toBytes(value + 1)));
                            value = NumberBytesConversion.intFromBytes(((VariableChunk) chunk2).getBody());
                            instance2.write(new VariableChunk(target.getName(), i, NumberBytesConversion.toBytes(value + 1)));
                        }
                    } finally {
                        instance1.unlock(target);
                        instance2.unlock(target);
                    }
                }
            }

            for (int i = 0; i < chunkIds.size(); i++) {
                final Chunk chunk1 = instance1.read(chunkIds.get(i));
                final Chunk chunk2 = instance2.read(chunkIds.get(i));
                Assert.assertEquals(chunk1, chunk2);
            }
        }

        try (final Storage instance1 = new FileStorage(root1, this.chunkSizeLimit, this.directoryBitSize);
                final Storage instance2 = new WriteCachingStorage(new FileStorage(root2, this.chunkSizeLimit, this.directoryBitSize),
                        (int) (this.factor * numOfChunks))) {
            instance1.registerChunk(0, VariableChunk.class, VariableChunk.getParser(), VariableChunk.Id.class, VariableChunk.Id.getParser());
            instance2.registerChunk(0, VariableChunk.class, VariableChunk.getParser(), VariableChunk.Id.class, VariableChunk.Id.getParser());
            for (final Chunk.Id<?> id : chunkIds) {
                Assert.assertEquals(instance1.read(id), instance2.read(id));
            }
        }

    }
}