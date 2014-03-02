/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import nippon.kawauso.chiraura.closet.Mountain;
import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.lib.math.MathFunctions;
import nippon.kawauso.chiraura.lib.test.FileFunctions;
import nippon.kawauso.chiraura.messenger.CryptographicKeys;
import nippon.kawauso.chiraura.network.AddressedPeer;
import nippon.kawauso.chiraura.storage.Chunk;
import nippon.kawauso.chiraura.storage.FileStorageTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class P2pClosetTest {

    private static final int FIRST_PORT = 10_000;
    private static final File ROOT = new File("tmp" + File.separator + P2pClosetTest.class.getSimpleName());

    private static final AtomicInteger portOffset = new AtomicInteger(0);

    /**
     * 初期化。
     */
    public P2pClosetTest() {
        LoggingFunctions.startLogging(Level.WARNING);
    }

    /**
     * 起動試験。
     * @throws Exception 異常
     */
    @Test
    public void testBoot() throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool();

        final File root = new File(ROOT, "boot");
        final KeyPair id = CryptographicKeys.newPublicKeyPair();
        final int port = FIRST_PORT + portOffset.getAndIncrement();
        final P2pCloset instance = new P2pCloset(new P2pCloset.Parameters(root, id, port, executor));
        instance.start(executor);

        Thread.sleep(100L);

        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        instance.close();
    }

    private static List<InetSocketAddress> getPeers(final int port) {
        return Arrays.asList(new InetSocketAddress[] { new InetSocketAddress("localhost", port) });
    }

    /**
     * 接続試験。
     * @throws Exception 異常
     */
    @Test
    public void testConnect() throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool();

        final File root1 = new File(ROOT, "connect1");
        final KeyPair id1 = CryptographicKeys.newPublicKeyPair();
        final int port1 = FIRST_PORT + portOffset.getAndIncrement();
        final P2pCloset instance1 = new P2pCloset(new P2pCloset.Parameters(root1, id1, port1, executor).setPortIgnore(false));
        instance1.start(executor);

        // 起動待ち。
        Thread.sleep(100L);

        // 先に起動中の方に接続させる方。
        final File root2 = new File(ROOT, "connect2");
        final KeyPair id2 = CryptographicKeys.newPublicKeyPair();
        final int port2 = FIRST_PORT + portOffset.getAndIncrement();
        final P2pCloset instance2 = new P2pCloset(new P2pCloset.Parameters(root2, id2, port2, executor).setPortIgnore(false).setPeers(getPeers(port1)));
        instance2.start(executor);

        // 安定待ち。
        Thread.sleep(Duration.SECOND);

        // 個体が保存されているか検査。
        final List<AddressedPeer> peers1 = instance1.getPeers();
        Assert.assertEquals(1, peers1.size());
        Assert.assertTrue(peers1.get(0).getPeer().getAddress().isLoopbackAddress());
        Assert.assertEquals(port2, peers1.get(0).getPeer().getPort());

        final List<AddressedPeer> peers2 = instance2.getPeers();
        Assert.assertEquals(1, peers2.size());
        Assert.assertTrue(peers2.get(0).getPeer().getAddress().isLoopbackAddress());
        Assert.assertEquals(port1, peers2.get(0).getPeer().getPort());

        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        instance1.close();
        instance2.close();
    }

    /**
     * 通信網構築試験。
     * @throws Exception 異常
     */
    @Test
    public void testNetwork1() throws Exception {
        testNetwork(1);
    }

    /**
     * 通信網構築試験。
     * @throws Exception 異常
     */
    @Test
    public void testNetwork2() throws Exception {
        testNetwork(2);
    }

    /**
     * 通信網構築試験。
     * @throws Exception 異常
     */
    @Test
    public void testNetwork3() throws Exception {
        testNetwork(3);
    }

    /**
     * 通信網構築試験。
     * @throws Exception 異常
     */
    @Test
    public void testNetwork4() throws Exception {
        testNetwork(4);
    }

    /**
     * 通信網が安定するのを待つ。
     * @param covers 個体
     * @param interval 個体の安定化処置の間隔
     * @throws InterruptedException 割り込まれた場合
     */
    private static void waitStable(final P2pCloset[] instances, final long interval) throws InterruptedException {
        final List<List<AddressedPeer>> previous = new ArrayList<>();
        for (int i = 0; i < instances.length; i++) {
            previous.add(instances[i].getPeers());
        }

        int stableCount = 0;
        while (true) {
            Thread.sleep(interval * 3 / 2);

            boolean stable = true;
            for (int i = 0; i < instances.length; i++) {
                final List<AddressedPeer> peers = instances[i].getPeers();
                if (!peers.equals(previous.get(i))) {
                    stable = false;
                    previous.set(i, peers);
                }
            }

            if (stable) {
                stableCount++;
            } else {
                stableCount = 0;
            }

            if (stableCount > MathFunctions.log2(instances.length)) {
                break;
            }
        }
    }

    private static String getLabel(final int all, final int n) {
        return all + "-" + String.format("%0" + ((int) Math.ceil(Math.log10(all))) + "d", n);
    }

    private static void testNetwork(final int bits) throws Exception {
        final int numOfPeers = 1 << bits;
        final int[] ports = new int[numOfPeers];
        final P2pCloset[] instances = new P2pCloset[numOfPeers];

        final ExecutorService executor = Executors.newCachedThreadPool();

        final long operationTimeout = 750L;
        final long maintenanceInterval = Duration.SECOND;
        final AddressCalculator calculator = new EquallyDivider();
        for (int i = 0; i < numOfPeers; i++) {
            final File root = new File(ROOT, "network" + getLabel(numOfPeers, i));
            final KeyPair id = CryptographicKeys.newPublicKeyPair();
            ports[i] = FIRST_PORT + portOffset.getAndIncrement();
            final P2pCloset.Parameters param = new P2pCloset.Parameters(root, id, ports[i], executor)
                    .setMaintenanceInterval(maintenanceInterval)
                    .setOperationTimeout(operationTimeout)
                    .setPeerCapacity(bits)
                    .setCalculator(calculator)
                    .setPortIgnore(false);
            if (i != 0) {
                // 初期個体を追加。
                param.setPeers(getPeers(ports[0]));
            }
            instances[i] = new P2pCloset(param);
            instances[i].start(executor);

            // 起動待ちの遅延。
            Thread.sleep(100L);
        }

        // 安定待ち。
        waitStable(instances, maintenanceInterval);

        // 出来上がった通信網の検査。
        for (int i = 0; i < numOfPeers; i++) {
            final Address base = calculator.calculate(instances[i].getId().getPublic());

            final List<AddressedPeer> peers = instances[i].getPeers();
            final Set<Address> addresses = new HashSet<>();
            for (final AddressedPeer peer : peers) {
                addresses.add(peer.getAddress());
            }

            for (int j = Address.SIZE - bits; j < Address.SIZE; j++) {
                if (!addresses.contains(base.addPowerOfTwo(j))) {
                    Assert.fail(base + " " + j + " " + base.addPowerOfTwo(j) + " " + ports[i] + " " + peers);
                }
            }
        }

        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        for (final P2pCloset instance : instances) {
            instance.close();
        }
    }

    private static void register(final P2pCloset instance) {
        instance.registerChunk(0, GrowingBytes.class, GrowingBytes.getParser(), GrowingBytes.Id.class, GrowingBytes.Id.getParser(),
                GrowingBytes.Entry.class, GrowingBytes.Entry.getParser());
    }

    private static final long cacheDuration = Duration.SECOND;

    private static P2pCloset[] beforeOperation(final int numOfPeers, final String label, final ExecutorService executor) throws InterruptedException {
        final P2pCloset[] instances = new P2pCloset[numOfPeers];

        final long operationTimeout = 750L;
        final long maintenanceInterval = Duration.SECOND;
        final long backupInterval = 100 * Duration.SECOND;
        int port0 = 0;
        for (int i = 0; i < numOfPeers; i++) {
            final File root = new File(ROOT, label + getLabel(numOfPeers, i));
            final KeyPair id = CryptographicKeys.newPublicKeyPair();
            final int port = FIRST_PORT + portOffset.getAndIncrement();
            final P2pCloset.Parameters param = new P2pCloset.Parameters(root, id, port, executor)
                    .setMaintenanceInterval(maintenanceInterval)
                    .setOperationTimeout(operationTimeout)
                    .setBackupInterval(backupInterval)
                    .setCacheDuration(cacheDuration)
                    .setPeerCapacity(numOfPeers)
                    .setPortIgnore(false);
            if (i == 0) {
                port0 = port;
            } else {
                param.setPeers(getPeers(port0));
            }

            // 倉庫の初期化。
            root.mkdirs();
            Assert.assertTrue(root.exists());
            FileFunctions.deleteContents(root);

            instances[i] = new P2pCloset(param);
            register(instances[i]);
            instances[i].start(executor);

            // 起動待ちの遅延。
            Thread.sleep(100L);
        }

        // 安定待ち。
        waitStable(instances, maintenanceInterval);

        return instances;
    }

    private static void afterOperation(final ExecutorService executor, final P2pCloset[] instances) throws InterruptedException, MyRuleException, IOException {
        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        for (final P2pCloset instance : instances) {
            instance.close();
        }
    }

    /**
     * データ片の取得試験。
     * @throws Exception 異常
     */
    @Test
    public void testGetOriginal20() throws Exception {
        testGetOriginal(20);
    }

    private static void testGetOriginal(final int numOfPeers) throws Exception {
        final String label = "getOriginal";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = beforeOperation(numOfPeers, label, executor);

        // データ片の追加。
        final Mountain chunk = new GrowingBytes(label);
        Assert.assertTrue(instances[0].addOriginal(chunk, Duration.SECOND).isSuccess());

        for (final P2pCloset instance : instances) {
            Assert.assertEquals(chunk, instance.getOriginal(chunk.getId(), Duration.SECOND).getChunk());
        }

        afterOperation(executor, instances);
    }

    /**
     * データ片のキャッシュを許す取得試験。
     * @throws Exception 異常
     */
    @Test
    public void testGetCache20() throws Exception {
        testGetCache(20);
    }

    private static void testGetCache(final int numOfPeers) throws Exception {
        final String label = "getCache";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = beforeOperation(numOfPeers, label, executor);

        // データ片の追加。
        final Mountain chunk = new GrowingBytes(label);
        Assert.assertTrue(instances[0].addOriginal(chunk, Duration.SECOND).isSuccess());

        for (final P2pCloset instance : instances) {
            Assert.assertEquals(chunk, instance.getCache(chunk.getId(), Duration.SECOND).getChunk());
        }

        for (final P2pCloset instance : instances) {
            Assert.assertEquals(chunk, instance.getLocal(chunk.getId()));
        }

        afterOperation(executor, instances);
    }

    /**
     * データ片の差分取得試験。
     * @throws Exception 異常
     */
    @Test
    public void testUpdateOriginal20() throws Exception {
        testUpdateOriginal(20);
    }

    private static void testUpdateOriginal(final int numOfPeers) throws Exception {
        final String label = "updateOriginal";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = beforeOperation(numOfPeers, label, executor);

        // データ片の追加。
        final Mountain chunk = new GrowingBytes(label, 0);
        chunk.patch(new GrowingBytes.Entry(1, new byte[] { 1 }));
        chunk.patch(new GrowingBytes.Entry(2, new byte[] { 2 }));
        chunk.patch(new GrowingBytes.Entry(3, new byte[] { 3 }));
        Assert.assertTrue(instances[0].addOriginal(chunk, Duration.SECOND).isSuccess());

        for (final P2pCloset instance : instances) {
            Assert.assertEquals(chunk.getDiffsAfter(1), instance.updateOriginal(chunk.getId(), 1, Duration.SECOND).getDiffs());
        }

        afterOperation(executor, instances);
    }

    /**
     * データ片の復号取得試験。
     * @throws Exception 異常
     */
    @Test
    public void testGetOrUpdateCache20() throws Exception {
        testGetOrUpdateCache(20);
    }

    private static void testGetOrUpdateCache(final int numOfPeers) throws Exception {
        final String label = "getOrUpdateCache";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = beforeOperation(numOfPeers, label, executor);

        // データ片の追加。
        final Mountain chunk = new GrowingBytes(label, 0);
        Assert.assertTrue(instances[0].addOriginal(chunk, Duration.SECOND).isSuccess());
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals(chunk, instances[0].getCache(chunk.getId(), Duration.SECOND).getChunk());
        }

        for (int i = 1; i <= 5; i++) {
            @SuppressWarnings("unchecked")
            final Chunk.Id<GrowingBytes> id = (Chunk.Id<GrowingBytes>) chunk.getId();
            final Mountain.Dust<GrowingBytes> diff = new GrowingBytes.Entry(i, new byte[] { (byte) i });
            Assert.assertTrue(instances[0].patchOriginal(id, diff, Duration.SECOND).isSuccess());
            chunk.patch(diff);
        }

        // キャッシュが腐るのを待つ。
        Thread.sleep(cacheDuration);

        for (final P2pCloset instance : instances) {
            Assert.assertEquals(chunk, instance.getOrUpdateCache(chunk.getId(), Duration.SECOND).getChunk());
        }

        for (final P2pCloset instance : instances) {
            Assert.assertEquals(chunk, instance.getLocal(chunk.getId()));
        }

        afterOperation(executor, instances);
    }

    /**
     * 即時バックアップの試験。
     * @throws Exception 異常
     */
    @Test
    public void testRapidBackup20() throws Exception {
        testRapidBackup(20);
    }

    private static void testRapidBackup(final int numOfPeers) throws Exception {
        final String label = "rapidBackup";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = beforeOperation(numOfPeers, label, executor);

        // LoggingFunctions.startDebugLogging();

        // データ片の追加。
        final Mountain chunk = new GrowingBytes(label);
        Assert.assertTrue(instances[0].addOriginal(chunk, Duration.SECOND).isSuccess());

        // 即時バックアップが終わるのを待つ。
        Thread.sleep(100L);

        // LoggingFunctions.startLogging();

        // 2 個体だけが所持していることの検査。
        int added = 0;
        for (final P2pCloset instance : instances) {
            if (chunk.equals(instance.getLocal(chunk.getId()))) {
                added++;
            }
        }
        Assert.assertEquals(2, added);

        afterOperation(executor, instances);
    }

    /**
     * データ片の追加試験。
     * @throws Exception 異常
     */
    @Test
    public void testAddOriginal20() throws Exception {
        testAddOriginal(20);
    }

    private static void testAddOriginal(final int numOfPeers) throws Exception {
        final String label = "addOriginal";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = beforeOperation(numOfPeers, label, executor);

        // LoggingFunctions.startDebugLogging();

        // データ片の追加。
        final Mountain chunk = new GrowingBytes(label);
        Assert.assertTrue(instances[0].addOriginal(chunk, Duration.SECOND).isSuccess());
        for (int i = 0; i < instances.length; i++) {
            Assert.assertFalse(instances[i].addOriginal(chunk, Duration.SECOND).isSuccess());
        }

        // 即時バックアップが終わるのを待つ。
        Thread.sleep(100L);

        // 2 個体だけが所持していることの検査。
        int added = 0;
        for (final P2pCloset instance : instances) {
            if (chunk.equals(instance.getLocal(chunk.getId()))) {
                added++;
            }
        }
        Assert.assertEquals(2, added);

        afterOperation(executor, instances);
    }

    /**
     * 管理者を探す。
     * @param instances 候補。長さ 1 以上が必要。
     * @param target 目的の論理位置
     * @return 候補の中での管理者の番号
     */
    private static int searchManager(final P2pCloset[] instances, final Address target) {
        int bestIndex = 0;
        Address bestDist = instances[0].getSelfAddress().distanceTo(target);
        for (int i = 1; i < instances.length; i++) {
            final Address dist = instances[i].getSelfAddress().distanceTo(target);
            if (dist.compareTo(bestDist) < 0) {
                bestIndex = i;
                bestDist = dist;
            }
        }
        return bestIndex;
    }

    /**
     * データ片の追加試験。
     * @throws Exception 異常
     */
    @Test
    public void testAddCache20() throws Exception {
        testAddCache(20);
    }

    private static void testAddCache(final int numOfPeers) throws Exception {
        final String label = "addCache";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = beforeOperation(numOfPeers, label, executor);

        // 管理者以外からデータ片を追加してキャッシュする。
        final Mountain chunk = new GrowingBytes(label);
        final int manager = searchManager(instances, chunk.getId().getAddress());
        Assert.assertTrue(instances[(manager + 1) % instances.length].addCache(chunk.copy(), Duration.SECOND).isSuccess());
        for (int i = 0; i < instances.length; i++) {
            Assert.assertFalse(instances[i].addCache(chunk, Duration.SECOND).isSuccess());
        }

        int added = 0;
        for (final P2pCloset instance : instances) {
            if (chunk.equals(instance.getLocal(chunk.getId()))) {
                added++;
            }
        }
        Assert.assertTrue(1 < added);

        afterOperation(executor, instances);
    }

    /**
     * データ片への差分適用試験。
     * @throws Exception 異常
     */
    @Test
    public void testPatchOriginal20() throws Exception {
        testPatchOriginal(20);
    }

    private static void testPatchOriginal(final int numOfPeers) throws Exception {
        final String label = "updateOriginal";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = beforeOperation(numOfPeers, label, executor);

        // データ片の追加。
        final GrowingBytes chunk = new GrowingBytes(label, 0);
        Assert.assertTrue(instances[0].addOriginal(chunk.copy(), Duration.SECOND).isSuccess());

        for (int i = 0; i < instances.length; i++) {
            final GrowingBytes.Entry diff = new GrowingBytes.Entry(i, new byte[] { (byte) i });
            Assert.assertTrue(instances[i].patchOriginal(chunk.getId(), diff, Duration.SECOND).isSuccess());
            chunk.patch(diff);
        }

        for (final P2pCloset instance : instances) {
            Assert.assertEquals(chunk, instance.getOriginal(chunk.getId(), Duration.SECOND).getChunk());
        }

        afterOperation(executor, instances);
    }

    /**
     * データ片への復号適用兼取得試験。
     * @throws Exception 異常
     */
    @Test
    public void testPatchOrAddAndGetCache20() throws Exception {
        testPatchOrAddAndGetCache(20);
    }

    private static void testPatchOrAddAndGetCache(final int numOfPeers) throws Exception {
        final String label = "patchOrAddAndGetCache";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = beforeOperation(numOfPeers, label, executor);

        // データ片の追加。
        final GrowingBytes chunk = new GrowingBytes(label, 0);
        Assert.assertTrue(instances[0].addOriginal(chunk.copy(), Duration.SECOND).isSuccess());

        // 差分を追加しながら結果の変遷を確認。
        for (int i = 0; i < instances.length; i++) {
            final GrowingBytes.Entry diff = new GrowingBytes.Entry(i, new byte[] { (byte) i });
            chunk.patch(diff);
            Assert.assertEquals(chunk, instances[i].patchOrAddAndGetCache(chunk, Duration.SECOND).getChunk());
        }

        // 差分が追加されないなら最終版が得られることを確認。
        for (int i = 0; i < instances.length; i++) {
            Assert.assertEquals(chunk, instances[i].patchOrAddAndGetCache(chunk, Duration.SECOND).getChunk());
        }

        // キャッシュされていることを確認。
        for (final P2pCloset instance : instances) {
            Assert.assertEquals(chunk, instance.getLocal(chunk.getId()));
        }

        afterOperation(executor, instances);
    }

    /**
     * データ片への差分適用兼復号取得試験。
     * @throws Exception 異常
     */
    @Test
    public void testPatchAndGetOrUpdateCache20() throws Exception {
        testPatchAndGetOrUpdateCache(20);
    }

    private static void testPatchAndGetOrUpdateCache(final int numOfPeers) throws Exception {
        final String label = "patchAndGetOrUpdateCache";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = beforeOperation(numOfPeers, label, executor);

        // データ片の追加。
        final GrowingBytes chunk = new GrowingBytes(label, 0);
        Assert.assertTrue(instances[0].addOriginal(chunk.copy(), 100L).isSuccess());

        // 差分を追加しながら結果の変遷を確認。
        for (int i = 0; i < instances.length; i++) {
            final GrowingBytes.Entry diff = new GrowingBytes.Entry(i, new byte[] { (byte) i });
            Assert.assertTrue(chunk.patch(diff));
            final PatchAndGetOrUpdateCacheResult result = instances[i].patchAndGetOrUpdateCache(chunk.getId(), diff, Duration.SECOND);
            Assert.assertTrue(result.isSuccess());
            Assert.assertEquals(chunk, result.getChunk());
        }

        // 差分が追加できないなら最終版が得られることを確認。
        for (int i = 0; i < instances.length; i++) {
            final GrowingBytes.Entry diff = new GrowingBytes.Entry(instances.length - 1, new byte[] { (byte) (instances.length - 1) });
            final PatchAndGetOrUpdateCacheResult result = instances[i].patchAndGetOrUpdateCache(chunk.getId(), diff, Duration.SECOND);
            Assert.assertFalse(result.isSuccess());
            Assert.assertEquals(chunk, result.getChunk());
        }

        // キャッシュされていることを確認。
        for (final P2pCloset instance : instances) {
            Assert.assertEquals(chunk, instance.getLocal(chunk.getId()));
        }

        afterOperation(executor, instances);
    }

    /**
     * 在庫確認試験。
     * @throws Exception 異常
     */
    @Test
    public void testCheckStock10() throws Exception {
        testCheckStock(10);
    }

    private static void testCheckStock(final int numOfPeers) throws Exception {
        final String label = "checkStock";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = beforeOperation(numOfPeers, label, executor);

        // 何も無ければ何も申告されないことを確認。
        for (int i = 0; i < instances.length; i++) {
            for (int j = 0; j < instances.length; j++) {
                if (j != i) {
                    Assert.assertEquals(new ArrayList<StockEntry>(0), instances[i].checkStock(instances[j].getSelf(), Duration.SECOND).getStockedEntries());
                }
            }
        }

        // データ片を管理者以外に追加して申告されることの確認。
        final GrowingBytes chunk = new GrowingBytes(label, 0);
        final int manager = searchManager(instances, chunk.getId().getAddress());
        instances[(manager + 1) % instances.length].addLocal(chunk.copy());
        CheckStockResult result = instances[manager].checkStock(instances[(manager + 1) % instances.length].getSelf(), Duration.SECOND);
        Assert.assertEquals(1, result.getStockedEntries().size());
        Assert.assertEquals(chunk.getId(), result.getStockedEntries().get(0).getId());
        Assert.assertEquals(chunk.getDate(), result.getStockedEntries().get(0).getDate());
        Assert.assertEquals(chunk.getHashValue(), result.getStockedEntries().get(0).getHashValue());

        // 管理者にも追加したら申告されなくなることの確認。
        instances[manager].addLocal(chunk);
        result = instances[manager].checkStock(instances[(manager + 1) % instances.length].getSelf(), Duration.SECOND);
        Assert.assertEquals(new ArrayList<StockEntry>(0), result.getStockedEntries());

        // 管理者以外の方が新しくなったら申告されることの確認。
        for (int i = 1; i < 5; i++) {
            final GrowingBytes.Entry diff = new GrowingBytes.Entry(i, new byte[] { (byte) i });
            chunk.patch(diff);
        }
        instances[(manager + 2) % instances.length].addLocal(chunk.copy());
        result = instances[manager].checkStock(instances[(manager + 2) % instances.length].getSelf(), Duration.SECOND);
        Assert.assertEquals(1, result.getStockedEntries().size());
        Assert.assertEquals(chunk.getId(), result.getStockedEntries().get(0).getId());
        Assert.assertEquals(chunk.getDate(), result.getStockedEntries().get(0).getDate());
        Assert.assertEquals(chunk.getHashValue(), result.getStockedEntries().get(0).getHashValue());

        afterOperation(executor, instances);
    }

    /**
     * 復元試験。
     * @throws Exception 異常
     */
    @Test
    public void testRecovery10() throws Exception {
        testRecovery(10);
    }

    private static void testRecovery(final int numOfPeers) throws Exception {
        final String label = "recovery";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = beforeOperation(numOfPeers, label, executor);

        // データ片。
        final GrowingBytes chunk = new GrowingBytes(label, 0);
        final int manager = searchManager(instances, chunk.getId().getAddress());
        instances[(manager + 1) % instances.length].addLocal(chunk.copy());
        CheckStockResult stockResult = instances[manager].checkStock(instances[(manager + 1) % instances.length].getSelf(), Duration.SECOND);
        Assert.assertEquals(1, stockResult.getStockedEntries().size());
        Assert.assertEquals(chunk.getId(), stockResult.getStockedEntries().get(0).getId());
        Assert.assertEquals(chunk.getDate(), stockResult.getStockedEntries().get(0).getDate());
        Assert.assertEquals(chunk.getHashValue(), stockResult.getStockedEntries().get(0).getHashValue());

        RecoveryResult result = instances[manager].recovery(stockResult.getStockedEntries().get(0), instances[(manager + 1) % instances.length].getSelf(),
                Duration.SECOND);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(chunk, instances[manager].getLocal(chunk.getId()));

        // 差分
        for (int i = 1; i < 5; i++) {
            final GrowingBytes.Entry diff = new GrowingBytes.Entry(i, new byte[] { (byte) i });
            chunk.patch(diff);
        }
        instances[(manager + 2) % instances.length].addLocal(chunk.copy());
        stockResult = instances[manager].checkStock(instances[(manager + 2) % instances.length].getSelf(), Duration.SECOND);
        Assert.assertEquals(1, stockResult.getStockedEntries().size());
        Assert.assertEquals(chunk.getId(), stockResult.getStockedEntries().get(0).getId());
        Assert.assertEquals(chunk.getDate(), stockResult.getStockedEntries().get(0).getDate());
        Assert.assertEquals(chunk.getHashValue(), stockResult.getStockedEntries().get(0).getHashValue());

        result = instances[manager].recovery(stockResult.getStockedEntries().get(0), instances[(manager + 2) % instances.length].getSelf(), Duration.SECOND);
        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(chunk, instances[manager].getLocal(chunk.getId()));

        afterOperation(executor, instances);
    }

    /**
     * 発注試験。
     * @throws Exception 異常
     */
    @Test
    public void testCheckDemand10() throws Exception {
        testCheckDemand(10);
    }

    private static void testCheckDemand(final int numOfPeers) throws Exception {
        final String label = "checkDemand";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = beforeOperation(numOfPeers, label, executor);

        // 何も無ければ何も要求されないことを確認。
        for (int i = 0; i < instances.length; i++) {
            for (int j = 0; j < instances.length; j++) {
                if (j != i) {
                    Assert.assertEquals(new ArrayList<DemandEntry>(0), instances[i].checkDemand(instances[j].getSelf(), Duration.SECOND).getDemandedEntries());
                }
            }
        }

        // 管理者だけがデータ片を持つと要求されることを確認。
        final GrowingBytes chunk = new GrowingBytes(label, 0);
        final int manager = searchManager(instances, chunk.getId().getAddress());
        instances[manager].addLocal(chunk.copy());
        for (int i = 0; i < instances.length; i++) {
            if (i != manager) {
                final CheckDemandResult result = instances[manager].checkDemand(instances[i].getSelf(), Duration.SECOND);
                Assert.assertEquals(1, result.getDemandedEntries().size());
                Assert.assertEquals(chunk.getId(), result.getDemandedEntries().get(0).getId());
                Assert.assertFalse(result.getDemandedEntries().get(0).isStocked());
            }
        }

        // 全員に持たせたら要求されなくなることの確認。
        for (final P2pCloset instance : instances) {
            instance.addLocal(chunk);
        }
        for (int i = 0; i < instances.length; i++) {
            if (i != manager) {
                final CheckDemandResult result = instances[manager].checkDemand(instances[i].getSelf(), Duration.SECOND);
                Assert.assertEquals(new ArrayList<DemandEntry>(0), result.getDemandedEntries());
            }
        }

        // 管理者だけがデータ片を持つと要求されることを確認。
        final HashValue firstHashValue = chunk.getHashValue();
        for (int i = 1; i < 5; i++) {
            final GrowingBytes.Entry diff = new GrowingBytes.Entry(i, new byte[] { (byte) i });
            chunk.patch(diff);
            instances[manager].patchLocal(chunk.getId(), diff);
        }
        for (int i = 0; i < instances.length; i++) {
            if (i != manager) {
                final CheckDemandResult result = instances[manager].checkDemand(instances[i].getSelf(), Duration.SECOND);
                Assert.assertEquals(1, result.getDemandedEntries().size());
                Assert.assertEquals(chunk.getId(), result.getDemandedEntries().get(0).getId());
                Assert.assertTrue(result.getDemandedEntries().get(0).isStocked());
                Assert.assertEquals(chunk.getFirstDate(), result.getDemandedEntries().get(0).getStockDate());
                Assert.assertEquals(firstHashValue, result.getDemandedEntries().get(0).getStockHashValue());
            }
        }

        afterOperation(executor, instances);
    }

    /**
     * 複製試験。
     * @throws Exception 異常
     */
    @Test
    public void testBackup10() throws Exception {
        testBackup(10);
    }

    private static void testBackup(final int numOfPeers) throws Exception {
        final String label = "backup";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = beforeOperation(numOfPeers, label, executor);

        // データ片複製。
        final GrowingBytes chunk = new GrowingBytes(label, 0);
        final int manager = searchManager(instances, chunk.getId().getAddress());
        instances[manager].addLocal(chunk.copy());
        for (int i = 0; i < instances.length; i++) {
            if (i != manager) {
                final CheckDemandResult demandResult = instances[manager].checkDemand(instances[i].getSelf(), Duration.SECOND);
                Assert.assertEquals(1, demandResult.getDemandedEntries().size());
                Assert.assertEquals(chunk.getId(), demandResult.getDemandedEntries().get(0).getId());
                Assert.assertFalse(demandResult.getDemandedEntries().get(0).isStocked());

                final BackupResult result = instances[manager].backup(demandResult.getDemandedEntries().get(0), instances[i].getSelf(), Duration.SECOND);
                Assert.assertTrue(result.isSuccess());
                Assert.assertEquals(chunk, instances[i].getLocal(chunk.getId()));
            }
        }

        // 差分複製。
        for (int i = 1; i < 5; i++) {
            final GrowingBytes.Entry diff = new GrowingBytes.Entry(i, new byte[] { (byte) i });
            chunk.patch(diff);
            instances[manager].patchLocal(chunk.getId(), diff);
        }
        for (int i = 0; i < instances.length; i++) {
            if (i != manager) {
                final CheckDemandResult demandResult = instances[manager].checkDemand(instances[i].getSelf(), Duration.SECOND);
                Assert.assertEquals(1, demandResult.getDemandedEntries().size());
                Assert.assertEquals(chunk.getId(), demandResult.getDemandedEntries().get(0).getId());
                Assert.assertTrue(demandResult.getDemandedEntries().get(0).isStocked());
                Assert.assertEquals(chunk.getFirstDate(), demandResult.getDemandedEntries().get(0).getStockDate());

                final BackupResult result = instances[manager].backup(demandResult.getDemandedEntries().get(0), instances[i].getSelf(), Duration.SECOND);
                Assert.assertTrue(result.isSuccess());
                Assert.assertEquals(chunk, instances[i].getLocal(chunk.getId()));
            }
        }

        afterOperation(executor, instances);
    }

    /**
     * いろいろ書き込んで 2 個体で同期されるかどうかを検査する。
     * @throws Exception 異常
     */
    @Test
    public void testSynchronization() throws Exception {
        final long operationTimeout = Duration.SECOND;
        final long maintenanceInterval = 2 * Duration.SECOND;
        final long backupInterval = 500L;

        final ExecutorService executor = Executors.newCachedThreadPool();

        final P2pCloset[] instances = new P2pCloset[2];

        int port0 = 0;
        for (int i = 0; i < instances.length; i++) {
            final File root = new File(ROOT, "backup" + i);
            final KeyPair id = CryptographicKeys.newPublicKeyPair();
            final int port = FIRST_PORT + portOffset.getAndIncrement();
            final P2pCloset.Parameters param = new P2pCloset.Parameters(root, id, port, executor)
                    .setMaintenanceInterval(maintenanceInterval)
                    .setBackupInterval(backupInterval)
                    .setOperationTimeout(operationTimeout)
                    .setPeerCapacity(1)
                    .setPortIgnore(false);
            if (i == 0) {
                port0 = port;
            } else {
                param.setPeers(getPeers(port0));
            }

            // 倉庫の初期化。
            root.mkdirs();
            Assert.assertTrue(root.exists());
            FileFunctions.deleteContents(root);

            instances[i] = new P2pCloset(param);
            register(instances[i]);
            instances[i].start(executor);

            // 起動待ちの遅延。
            Thread.sleep(100L);
        }

        // 安定待ち。
        waitStable(instances, maintenanceInterval);

        // データ片の追加。
        final Mountain[] chunks = new Mountain[100];
        for (int i = 0; i < chunks.length; i++) {
            chunks[i] = new GrowingBytes(Integer.toString(i));
            instances[i % instances.length].addChunk(chunks[i], operationTimeout);
        }

        // LoggingFunctions.startDebugLogging();

        // 同期待ち。
        Thread.sleep((long) (backupInterval * (3.0 / 2.0) * 2 * MathFunctions.log2(instances.length)));

        for (int i = 0; i < chunks.length; i++) {
            for (int j = 0; j < instances.length; j++) {
                Assert.assertEquals(chunks[i], instances[j].getLocal(chunks[i].getId()));
            }
        }

        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        for (final P2pCloset instance : instances) {
            instance.close();
        }
    }

    /**
     * 通信網更新試験。
     * @throws Exception 異常
     */
    @Test
    public void testLostPeer3() throws Exception {
        testLostPeer(3, 3);
    }

    /**
     * 通信網更新試験。
     * @throws Exception 異常
     */
    @Test
    public void testLostPeer4() throws Exception {
        testLostPeer(4, 4);
    }

    /**
     * 通信網更新試験。
     * @throws Exception 異常
     */
    @Test
    public void testLostPeer10() throws Exception {
        testLostPeer(10, 10);
    }

    /**
     * 通信網更新試験。
     * @throws Exception 異常
     */
    @Test
    public void testLostPeer20() throws Exception {
        testLostPeer(20, 20);
    }

    /**
     * 通信網更新試験。
     * @throws Exception 異常
     */
    // @Test
    public void testLostPeer50() throws Exception {
        testLostPeer(50, 50);
    }

    private static void testLostPeer(final int numOfPeers, final int numOfLoops) throws Exception {
        final P2pCloset[] instances = new P2pCloset[numOfPeers];

        final ExecutorService executor = Executors.newCachedThreadPool();
        final ExecutorService lostPeerExecutor = Executors.newCachedThreadPool();

        final int lostPeerIndex = 1 + ThreadLocalRandom.current().nextInt(numOfPeers - 1);

        final long operationTimeout = 750L;
        final long maintenanceInterval = Duration.SECOND;
        int port0 = 0;
        int lostPort = 0;
        for (int i = 0; i < numOfPeers; i++) {
            final File root = new File(ROOT, "lost" + getLabel(numOfPeers, i));
            final KeyPair id = CryptographicKeys.newPublicKeyPair();
            final int port = FIRST_PORT + portOffset.getAndIncrement();
            final P2pCloset.Parameters param = new P2pCloset.Parameters(root, id, port, (i == lostPeerIndex ? lostPeerExecutor : executor))
                    .setMaintenanceInterval(maintenanceInterval)
                    .setBackupInterval(maintenanceInterval)
                    .setOperationTimeout(operationTimeout)
                    .setPeerCapacity(numOfPeers)
                    .setPortIgnore(false);
            if (i == 0) {
                port0 = port;
            } else {
                // 初期個体を追加。
                param.setPeers(getPeers(port0));
            }
            if (i == lostPeerIndex) {
                lostPort = port;
            }
            instances[i] = new P2pCloset(param);
        }

        for (int i = 0; i < numOfPeers; i++) {
            if (i == lostPeerIndex) {
                instances[i].start(lostPeerExecutor);
            } else {
                instances[i].start(executor);
            }

            // 起動待ちの遅延。
            Thread.sleep(100L);
        }

        // 安定待ち。
        waitStable(instances, maintenanceInterval);

        // LoggingFunctions.startDebugLogging();

        // 1 つ殺す。
        lostPeerExecutor.shutdownNow();
        Assert.assertTrue(lostPeerExecutor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));

        for (int i = 0; i < numOfLoops; i++) {
            Thread.sleep((long) (maintenanceInterval * 1.01));

            // 殺した奴が通信網から消えているか検査。
            boolean completed = true;
            final InetSocketAddress lostPeer = new InetSocketAddress("localhost", lostPort);
            for (int j = 0; completed && j < numOfPeers; j++) {
                if (j == lostPeerIndex) {
                    continue;
                }
                for (final AddressedPeer peer : instances[j].getImportantPeers()) {
                    if (peer.getPeer().equals(lostPeer)) {
                        completed = false;
                        break;
                    }
                }
            }

            // System.out.println("Loop " + i);

            if (completed) {
                break;
            } else {
                if (i > numOfLoops - 1) {
                    Assert.fail();
                }
            }
        }

        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        for (final P2pCloset instance : instances) {
            instance.close();
        }
    }

    /**
     * 異常復元の試験。
     * @throws Exception 異常
     */
    @Test
    public void testErrorRecovery20() throws Exception {
        testErrorRecovery(20);
    }

    private static void testErrorRecovery(final int numOfPeers) throws Exception {
        final String label = "errorRecovery";
        final ExecutorService executor = Executors.newCachedThreadPool();
        final P2pCloset[] instances = new P2pCloset[numOfPeers];
        final File[] roots = new File[numOfPeers];

        final int directoryBitSize = 8;
        final long operationTimeout = 750L;
        final long maintenanceInterval = Duration.SECOND;
        final long backupInterval = 100 * Duration.SECOND;
        int port0 = 0;
        for (int i = 0; i < numOfPeers; i++) {
            final File root = new File(ROOT, label + getLabel(numOfPeers, i));
            final KeyPair id = CryptographicKeys.newPublicKeyPair();
            final int port = FIRST_PORT + portOffset.getAndIncrement();
            final P2pCloset.Parameters param = new P2pCloset.Parameters(root, id, port, executor)
                    .setStorageDirectoryBitSize(directoryBitSize)
                    .setMaintenanceInterval(maintenanceInterval)
                    .setOperationTimeout(operationTimeout)
                    .setBackupInterval(backupInterval)
                    .setCacheDuration(cacheDuration)
                    .setChunkCacheCapacity(0)
                    .setPeerCapacity(numOfPeers)
                    .setPortIgnore(false);
            if (i == 0) {
                port0 = port;
            } else {
                param.setPeers(getPeers(port0));
            }

            // 倉庫の初期化。
            root.mkdirs();
            Assert.assertTrue(root.exists());
            FileFunctions.deleteContents(root);

            roots[i] = root;
            instances[i] = new P2pCloset(param);
            register(instances[i]);
            instances[i].start(executor);

            // 起動待ちの遅延。
            Thread.sleep(100L);
        }

        // 安定待ち。
        waitStable(instances, maintenanceInterval);

        // データ片の追加。
        final Mountain chunk = new GrowingBytes(label);
        Assert.assertTrue(instances[0].addOriginal(chunk, Duration.SECOND).isSuccess());
        for (final P2pCloset instance : instances) {
            instance.getCache(chunk.getId(), operationTimeout);
        }

        final int manager = searchManager(instances, chunk.getId().getAddress());
        final File file = FileStorageTest.getFile(roots[manager], directoryBitSize, chunk.getId(), 0);
        Assert.assertTrue(file.exists());

        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
            output.write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, });
        }

        LoggingFunctions.startLogging(Level.SEVERE);

        Assert.assertNull(instances[manager].getLocal(chunk.getId()));

        // 復元待ち。
        Thread.sleep(100L);

        Assert.assertEquals(chunk, instances[manager].getLocal(chunk.getId()));

        afterOperation(executor, instances);
    }
}
