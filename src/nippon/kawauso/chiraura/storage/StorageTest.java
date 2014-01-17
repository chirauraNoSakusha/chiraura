/**
 * 
 */
package nippon.kawauso.chiraura.storage;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.container.Pair;
import nippon.kawauso.chiraura.lib.converter.NumberBytesConversion;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.lib.test.BiasedRandom;

import org.junit.Assert;

/**
 * @author chirauraNoSakusha
 */
final class StorageTest {

    private static final Logger LOG = Logger.getLogger(StorageTest.class.getName());

    static void testMinimum(final Storage instance) throws IOException, InterruptedException, MyRuleException {
        instance.registerChunk(0, ConstantChunk.class, ConstantChunk.getParser(), ConstantChunk.Id.class, ConstantChunk.Id.getParser());

        // データ片を準備して、
        final Chunk chunk = new ConstantChunk(0, "a".getBytes());

        // 書いて、
        Assert.assertTrue(instance.write(chunk));

        // 存在確認して、
        Assert.assertTrue(instance.contains(chunk.getId()));

        // 読んで、
        Assert.assertEquals(chunk, instance.read(chunk.getId()));

        // チラ見して、
        Assert.assertEquals(new SimpleIndex(chunk), instance.getIndex(chunk.getId()));

        // 全域チラ見して、
        Map<Chunk.Id<?>, Storage.Index> indices = instance.getIndices(Address.ZERO, Address.MAX);
        Assert.assertEquals(1, indices.size());
        Assert.assertEquals(new SimpleIndex(chunk), indices.values().iterator().next());

        // 一点チラ見して、
        indices = instance.getIndices(chunk.getId().getAddress(), chunk.getId().getAddress());
        Assert.assertEquals(1, indices.size());
        Assert.assertEquals(new SimpleIndex(chunk), indices.values().iterator().next());

        // 除外チラ見して、
        if (Address.ZERO.compareTo(chunk.getId().getAddress()) < 0) {
            indices = instance.getIndices(Address.ZERO, chunk.getId().getAddress().subtractOne());
            Assert.assertEquals(0, indices.size());
        }
        if (chunk.getId().getAddress().compareTo(Address.MAX) < 0) {
            indices = instance.getIndices(chunk.getId().getAddress().addPowerOfTwo(0), Address.MAX);
            Assert.assertEquals(0, indices.size());
        }

        // 無駄に書いて、
        Assert.assertFalse(instance.write(chunk));

        // 消して、
        Assert.assertTrue(instance.delete(chunk.getId()));

        // 存在確認して、
        Assert.assertFalse(instance.contains(chunk.getId()));

        // 読んで、
        Assert.assertNull(instance.read(chunk.getId()));

        // チラ見して、
        Assert.assertNull(instance.getIndex(chunk.getId()));

        // 全域チラ見して、
        indices = instance.getIndices(Address.ZERO, Address.MAX);
        Assert.assertEquals(0, indices.size());

        // 一点チラ見して、
        indices = instance.getIndices(chunk.getId().getAddress(), chunk.getId().getAddress());
        Assert.assertEquals(0, indices.size());

        // 除外チラ見して、
        if (Address.ZERO.compareTo(chunk.getId().getAddress()) < 0) {
            indices = instance.getIndices(Address.ZERO, chunk.getId().getAddress().subtractOne());
            Assert.assertEquals(0, indices.size());
        }
        if (chunk.getId().getAddress().compareTo(Address.MAX) < 0) {
            indices = instance.getIndices(chunk.getId().getAddress().addPowerOfTwo(0), Address.MAX);
            Assert.assertEquals(0, indices.size());
        }

        // 書いて、
        instance.forceWrite(chunk);

        // 存在確認して、
        Assert.assertTrue(instance.contains(chunk.getId()));

        // 読んで、
        Assert.assertEquals(chunk, instance.read(chunk.getId()));

        // チラ見して、
        Assert.assertEquals(new SimpleIndex(chunk), instance.getIndex(chunk.getId()));

        // 全域チラ見して、
        indices = instance.getIndices(Address.ZERO, Address.MAX);
        Assert.assertEquals(1, indices.size());
        Assert.assertEquals(new SimpleIndex(chunk), indices.values().iterator().next());

        // 一点チラ見して、
        indices = instance.getIndices(chunk.getId().getAddress(), chunk.getId().getAddress());
        Assert.assertEquals(1, indices.size());
        Assert.assertEquals(new SimpleIndex(chunk), indices.values().iterator().next());

        // 除外チラ見して、
        if (Address.ZERO.compareTo(chunk.getId().getAddress()) < 0) {
            indices = instance.getIndices(Address.ZERO, chunk.getId().getAddress().subtractOne());
            Assert.assertEquals(0, indices.size());
        }
        if (chunk.getId().getAddress().compareTo(Address.MAX) < 0) {
            indices = instance.getIndices(chunk.getId().getAddress().addPowerOfTwo(0), Address.MAX);
            Assert.assertEquals(0, indices.size());
        }

        // 無駄に書いて、
        Assert.assertFalse(instance.write(chunk));
        instance.close();
    }

    /**
     * 非並列な対照検査。
     * @param instance1 1 つ目の被験体
     * @param instance2 2 つ目の被験体
     * @param numOfLoops 繰り返し回数
     * @param numOfChunks 使うデータ片の数
     * @throws Exception 異常
     */
    static void testRandom(final Storage instance1, final Storage instance2, final int numOfLoops, final int numOfChunks) throws Exception {
        final Random uniform = new Random();
        final BiasedRandom bias = new BiasedRandom(uniform);

        instance1.registerChunk(0, VariableChunk.class, VariableChunk.getParser(), VariableChunk.Id.class, VariableChunk.Id.getParser());
        instance2.registerChunk(0, VariableChunk.class, VariableChunk.getParser(), VariableChunk.Id.class, VariableChunk.Id.getParser());

        // データ片の準備。
        final List<VariableChunk.Id> chunkIds = new ArrayList<>(numOfChunks);
        for (int i = 0; i < numOfChunks; i++) {
            chunkIds.add(new VariableChunk.Id(Integer.toString(i)));
        }
        // 概要の範囲の準備。
        final BigInteger width = Address.MAX.toBigInteger().divide(BigInteger.valueOf(numOfChunks));
        final List<Pair<Address, Address>> ranges = new ArrayList<>(numOfChunks);
        for (int i = 0; i < numOfChunks; i++) {
            final BigInteger start = width.multiply(BigInteger.valueOf(uniform.nextInt(numOfChunks)));
            final BigInteger end = start.add(width.multiply(BigInteger.valueOf((long) (1 + 3 * bias.next())))).subtract(BigInteger.ONE);
            if (Address.MAX.toBigInteger().compareTo(end) <= 0) {
                ranges.add(new Pair<>(new Address(start, Address.SIZE), Address.MAX));
            } else {
                ranges.add(new Pair<>(new Address(start, Address.SIZE), new Address(end, Address.SIZE)));
            }
        }

        for (int i = 0; i < numOfLoops; i++) {
            final double flag = uniform.nextDouble();
            if (flag < 1.0 / 8.0) {
                // contains.
                final VariableChunk.Id target = chunkIds.get((int) (bias.next() * chunkIds.size()));
                Assert.assertEquals(instance1.contains(target), instance2.contains(target));
            } else if (flag < 2.0 / 8.0) {
                // getIndex.
                final VariableChunk.Id target = chunkIds.get((int) (bias.next() * chunkIds.size()));
                Assert.assertEquals(instance1.getIndex(target), instance2.getIndex(target));
            } else if (flag < 3.0 / 8.0) {
                // getIndices.
                final int a = (int) (bias.next() * ranges.size());
                final Pair<Address, Address> target = ranges.get(a);
                Assert.assertEquals(instance1.getIndices(target.getFirst(), target.getSecond()), instance2.getIndices(target.getFirst(), target.getSecond()));
            } else if (flag < 4.0 / 8.0) {
                // read.
                final VariableChunk.Id target = chunkIds.get((int) (bias.next() * chunkIds.size()));
                Assert.assertEquals(instance1.read(target), instance2.read(target));
            } else if (flag < 5.0 / 8.0) {
                // write.
                final VariableChunk.Id target = chunkIds.get((int) (bias.next() * chunkIds.size()));
                final Chunk chunk = new VariableChunk(target.getName(), i, NumberBytesConversion.toBytes(0));
                Assert.assertEquals(instance1.write(chunk), instance2.write(chunk));
            } else if (flag < 6.0 / 8.0) {
                // forceWrite.
                final VariableChunk.Id target = chunkIds.get((int) (bias.next() * chunkIds.size()));
                final Chunk chunk = new VariableChunk(target.getName(), i, NumberBytesConversion.toBytes(0));
                instance1.forceWrite(chunk);
                instance2.forceWrite(chunk);
            } else if (flag < 7.0 / 8.0) {
                // delete.
                final VariableChunk.Id target = chunkIds.get((int) (bias.next() * chunkIds.size()));
                Assert.assertEquals(instance1.delete(target), instance2.delete(target));
            } else {
                // 書き換え。
                final VariableChunk.Id target = chunkIds.get((int) (bias.next() * chunkIds.size()));
                instance1.lock(target);
                instance2.lock(target);
                try {
                    final VariableChunk chunk1 = instance1.read(target);
                    final VariableChunk chunk2 = instance2.read(target);
                    Assert.assertEquals(chunk1, chunk2);
                    if (chunk1 != null) {
                        int value;
                        value = NumberBytesConversion.intFromBytes(chunk1.getBody());
                        instance1.write(new VariableChunk(target.getName(), i, NumberBytesConversion.toBytes(value + 1)));
                        value = NumberBytesConversion.intFromBytes(chunk2.getBody());
                        instance2.write(new VariableChunk(target.getName(), i, NumberBytesConversion.toBytes(value + 1)));
                    }
                } finally {
                    instance1.unlock(target);
                    instance2.unlock(target);
                }
            }
        }

        for (int i = 0; i < chunkIds.size(); i++) {
            final Storage.Index index1 = instance1.getIndex(chunkIds.get(i));
            final Storage.Index index2 = instance2.getIndex(chunkIds.get(i));
            Assert.assertEquals(index1, index2);
            final Chunk chunk1 = instance1.read(chunkIds.get(i));
            final Chunk chunk2 = instance2.read(chunkIds.get(i));
            Assert.assertEquals(chunk1, chunk2);
        }

        instance1.close();
        instance2.close();
    }

    /**
     * ランダムに作成した ConstantChunk による性能検査。
     * @param instance 被験体
     * @param numOfLoops 繰り返し回数
     * @param numOfChunks データ片数
     * @param numOfProcesses プロセス数
     * @param chunkSize データ片サイズ
     * @param logPrefix 表示の前文
     * @throws Exception 異常
     */
    static void testConcurrencyPerformanceByConstantChunk(final Storage instance, final int numOfLoops, final int numOfChunks, final int numOfProcesses,
            final int chunkSize, final String logPrefix) throws Exception {
        final Random random = new Random();

        instance.registerChunk(0, ConstantChunk.class, ConstantChunk.getParser(), ConstantChunk.Id.class, ConstantChunk.Id.getParser());

        // データ片の準備。
        final Set<Chunk> pot = new HashSet<>();
        while (pot.size() < numOfChunks) {
            final byte[] buff = new byte[chunkSize];
            random.nextBytes(buff);
            pot.add(new ConstantChunk(pot.size(), buff));
        }
        final List<Chunk> chunks = new ArrayList<>(pot); // 読むだけなので同期しなくていい。
        // 概要の範囲の準備。
        final BigInteger width = Address.MAX.toBigInteger().divide(BigInteger.valueOf(numOfChunks));
        final List<Pair<Address, Address>> ranges = new ArrayList<>(numOfChunks);
        final BiasedRandom b = new BiasedRandom(random);
        for (int i = 0; i < numOfChunks; i++) {
            final BigInteger start = width.multiply(BigInteger.valueOf(random.nextInt(numOfChunks)));
            final BigInteger end = start.add(width.multiply(BigInteger.valueOf((long) (1 + 3 * b.next())))).subtract(BigInteger.ONE);
            if (Address.MAX.toBigInteger().compareTo(end) <= 0) {
                ranges.add(new Pair<>(new Address(start, Address.SIZE), Address.MAX));
            } else {
                ranges.add(new Pair<>(new Address(start, Address.SIZE), new Address(end, Address.SIZE)));
            }
        }

        // プロセスの準備。
        final Collection<Callable<Integer>> processes = new ArrayList<>(numOfProcesses);
        for (int i = 0; i < numOfProcesses; i++) {
            processes.add(new Reporter<Integer>(Level.SEVERE) {
                /*
                 * contains() と read() と write() をてきとうにするだけ。
                 */
                @Override
                public Integer subCall() throws Exception {
                    final Random uniform = ThreadLocalRandom.current();
                    final BiasedRandom bias = new BiasedRandom(uniform);
                    int success = 0;
                    int failure = 0;
                    for (int j = 0; j < numOfLoops; j++) {
                        final double flag = uniform.nextDouble();
                        if (flag < 1.0 / 7.0) {
                            // contains.
                            final Chunk target = chunks.get((int) (bias.next() * chunks.size()));
                            if (instance.contains(target.getId())) {
                                success++;
                            } else {
                                failure++;
                            }
                        } else if (flag < 2.0 / 7.0) {
                            // getIndex.
                            final Chunk target = chunks.get((int) (bias.next() * chunks.size()));
                            final Storage.Index index = instance.getIndex(target.getId());
                            if (index != null) {
                                success++;
                            } else {
                                failure++;
                            }
                        } else if (flag < 3.0 / 7.0) {
                            // getIndices.
                            final Pair<Address, Address> target = ranges.get((int) (bias.next() * ranges.size()));
                            final Map<Chunk.Id<?>, Storage.Index> indices = instance.getIndices(target.getFirst(), target.getSecond());
                            if (!indices.isEmpty()) {
                                success++;
                            } else {
                                failure++;
                            }
                        } else if (flag < 4.0 / 7.0) {
                            // read.
                            final Chunk target = chunks.get((int) (bias.next() * chunks.size()));
                            final Chunk chunk = instance.read(target.getId());
                            if (chunk != null) {
                                success++;
                            } else {
                                failure++;
                            }
                        } else if (flag < 5.0 / 7.0) {
                            // write.
                            final Chunk target = chunks.get((int) (bias.next() * chunks.size()));
                            if (instance.write(target)) {
                                success++;
                            } else {
                                failure++;
                            }
                        } else if (flag < 6.0 / 7.0) {
                            // forceWrite.
                            final Chunk target = chunks.get((int) (bias.next() * chunks.size()));
                            instance.forceWrite(target);
                            success++;
                        } else {
                            // delete.
                            final Chunk target = chunks.get((int) (bias.next() * chunks.size()));
                            if (instance.delete(target.getId())) {
                                success++;
                            } else {
                                failure++;
                            }
                        }
                    }

                    return success + failure;
                }
            });
        }

        // 実行。
        final ExecutorService executor = Executors.newFixedThreadPool(processes.size());
        final long start = System.nanoTime();
        final List<Future<Integer>> futures = executor.invokeAll(processes);
        final long end = System.nanoTime();

        int sum = 0;
        for (final Future<Integer> future : futures) {
            sum += future.get();
        }
        Assert.assertEquals(numOfProcesses * numOfLoops, sum);

        final double unitCost = (end - start) / (1_000_000.0 * numOfProcesses * numOfLoops);
        LOG.log(Level.SEVERE, "{0} 繰り返し回数: {1} プロセス数: {2} データ片数: {3} データ片サイズ: {4} 単位消費ミリ秒: {5} チェックサム: {6}",
                new Object[] { logPrefix, numOfLoops, numOfProcesses, numOfChunks, chunkSize, String.format("%f", unitCost), sum });

        instance.close();
    }

    /**
     * VariableChunk を用いた同期周りの検査。
     * @param instance 被験体
     * @param numOfLoops 繰り返し回数
     * @param numOfChunks データ片数
     * @param numOfProcesses プロセス数
     * @param logPrefix 表示の前文
     * @throws Exception 異常
     */
    static void testConcurrencyByVariableChunk(final Storage instance, final int numOfLoops, final int numOfChunks, final int numOfProcesses,
            final String logPrefix) throws Exception {

        instance.registerChunk(0, VariableChunk.class, VariableChunk.getParser(), VariableChunk.Id.class, VariableChunk.Id.getParser());

        // データ片の準備。
        final List<VariableChunk.Id> chunkIds = new ArrayList<>(numOfChunks); // 読むだけなので同期しなくていい。
        for (int i = 0; i < numOfChunks; i++) {
            chunkIds.add(new VariableChunk.Id(Integer.toString(i)));
        }

        // プロセスの準備。
        final Collection<Callable<Void>> processes = new ArrayList<>(numOfProcesses);
        for (int i = 0; i < numOfProcesses; i++) {
            processes.add(new Reporter<Void>(Level.SEVERE) {
                /*
                 * データ片の中身をインクリメントするだけ。
                 */
                @Override
                public Void subCall() throws Exception {
                    final Random uniform = ThreadLocalRandom.current();
                    final BiasedRandom bias = new BiasedRandom(uniform);
                    int count = 0;
                    while (count < numOfLoops) {
                        final VariableChunk.Id target = chunkIds.get((int) (bias.next() * chunkIds.size()));

                        if (!instance.contains(target)) {
                            // データ片の作成。
                            instance.lock(target);
                            try {
                                if (!instance.contains(target)) {
                                    instance.write(new VariableChunk(target.getName(), System.currentTimeMillis(), NumberBytesConversion.toBytes(0)));
                                }
                            } finally {
                                instance.unlock(target);
                            }
                        }

                        // 書き込みのためのロック。
                        if (uniform.nextDouble() < 0.5) {
                            instance.lock(target);
                        } else {
                            if (!instance.tryLock(target)) {
                                continue;
                            }
                        }
                        try {
                            final VariableChunk chunk = instance.read(target);
                            Assert.assertNotNull(chunk);
                            final int value = NumberBytesConversion.intFromBytes(chunk.getBody());
                            instance.write(new VariableChunk(target.getName(), System.currentTimeMillis(), NumberBytesConversion.toBytes(value + 1)));
                        } finally {
                            instance.unlock(target);
                        }

                        count++;
                    }
                    return null;
                }
            });
        }

        // 実行。
        final ExecutorService executor = Executors.newFixedThreadPool(processes.size());
        final long start = System.nanoTime();
        final List<Future<Void>> futures = executor.invokeAll(processes);
        final long end = System.nanoTime();

        for (final Future<Void> future : futures) {
            future.get();
        }

        int sum = 0;
        for (int i = 0; i < chunkIds.size(); i++) {
            final VariableChunk chunk = instance.read(chunkIds.get(i));
            Assert.assertNotNull(chunk);
            sum += NumberBytesConversion.intFromBytes(chunk.getBody());
        }
        Assert.assertEquals(numOfProcesses * numOfLoops, sum);

        final double unitCost = (end - start) / (1_000_000.0 * numOfProcesses * numOfLoops);
        LOG.log(Level.SEVERE, "{0} 繰り返し回数: {1} プロセス数: {2} データ片数: {3} 単位消費ミリ秒: {4} チェックサム: {5}",
                new Object[] { logPrefix, numOfLoops, numOfProcesses, numOfChunks, String.format("%f", unitCost), sum });

        instance.close();
    }
}
