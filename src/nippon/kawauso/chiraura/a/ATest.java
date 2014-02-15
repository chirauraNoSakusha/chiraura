/**
 * 
 */
package nippon.kawauso.chiraura.a;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.bbs.Client;
import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.lib.math.MathFunctions;
import nippon.kawauso.chiraura.lib.process.Reporter;
import nippon.kawauso.chiraura.lib.test.FileFunctions;
import nippon.kawauso.chiraura.network.AddressedPeer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class ATest {

    static {
        LogInitializer.init();
    }

    private static final int FIRST_PORT = 10_000;
    private static final File ROOT = new File("tmp" + File.separator + ATest.class.getSimpleName());

    private static final AtomicInteger portOffset = new AtomicInteger(0);

    /**
     * 初期化。
     */
    public ATest() {
        LoggingFunctions.startLogging(Level.SEVERE);
    }

    /**
     * 終処理。
     */
    @After
    public void after() {
        LogInitializer.reset();
    }

    /**
     * ヘルプの表示検査。
     * @throws Exception 異常
     */
    // @Test
    public void testHelp() throws Exception {
        A.main("-help -port 9999".split(" "));
    }

    /**
     * 起動試験。
     * @throws Exception 異常
     */
    @Test
    public void testBoot() throws Exception {
        final ExecutorService executor = Executors.newCachedThreadPool();

        final File root = new File(ATest.ROOT, "boot");
        final int port = FIRST_PORT + portOffset.getAndIncrement();
        final int bbsPort = FIRST_PORT + portOffset.getAndIncrement();
        executor.submit(new Reporter<Void>(Level.INFO) {
            @Override
            public Void subCall() throws Exception {
                A.main(("-root " + root.getPath() +
                        " -port " + Integer.toString(port) +
                        " -bbsPort " + Integer.toString(bbsPort) +
                        " -consoleLogLevel " + Level.WARNING.getName() +
                        " -fileLogLevel " + Level.OFF.getName()).split(" "));
                return null;
            }
        });

        Thread.sleep(3 * Duration.SECOND);

        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
    }

    private static final class CoverWrapper extends Reporter<Void> {
        private final A base;

        private CoverWrapper(final A base) {
            super(Level.WARNING);
            this.base = base;
        }

        @Override
        protected Void subCall() throws Exception {
            this.base.execute();
            return null;
        }
    }

    /**
     * 通信網が安定するのを待つ。
     * @param covers 個体
     * @param interval 個体の安定化処置の間隔
     * @throws InterruptedException 割り込まれた場合
     */
    private static void waitStable(final A[] covers, final long interval) throws InterruptedException {
        final List<List<AddressedPeer>> previous = new ArrayList<>();
        for (int i = 0; i < covers.length; i++) {
            previous.add(covers[i].getPeers());
        }
        int stableCount = 0;
        while (true) {
            Thread.sleep(interval * 3 / 2);
            boolean stable = true;
            for (int i = 0; i < covers.length; i++) {
                final List<AddressedPeer> peers = covers[i].getPeers();
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
            if (stableCount > MathFunctions.log2(covers.length)) {
                break;
            }
        }
    }

    /**
     * 操作試験。
     * @throws Exception 異常
     */
    @Test
    public void testOperation2() throws Exception {
        testOperation(2);
    }

    /**
     * 操作試験。
     * @throws Exception 異常
     */
    @Test
    public void testOperation3() throws Exception {
        testOperation(3);
    }

    /**
     * 操作試験。
     * @throws Exception 異常
     */
    @Test
    public void testOperation10() throws Exception {
        testOperation(10);
    }

    /**
     * 操作試験。
     * @throws Exception 異常
     */
    @Test
    public void testOperation20() throws Exception {
        testOperation(20);
    }

    private static void testOperation(final int numOfPeers) throws Exception {
        final Environment[] environments = new Environment[numOfPeers];
        final A[] covers = new A[numOfPeers];

        final ExecutorService executor = Executors.newCachedThreadPool();

        final long operationTimeout = 750L;
        final long maintenanceInterval = Duration.SECOND;
        for (int i = 0; i < numOfPeers; i++) {
            final File root = new File(ROOT, "operation" + Integer.toString(numOfPeers) + "-"
                    + String.format("%0" + Integer.toString((int) Math.ceil(Math.log10(numOfPeers))) + "d", i));
            final int port = FIRST_PORT + portOffset.getAndIncrement();
            final int bbsPort = FIRST_PORT + portOffset.getAndIncrement();
            (new File(root, "peers.txt")).delete();
            final File resource = new File(root, "resource");
            (new File(resource, "addressedPeers.dat")).delete();
            (new File(resource, "peers.dat")).delete();
            if (i != 0) {
                // 初期個体を追加。
                resource.mkdirs();
                Assert.assertTrue(resource.exists());
                final File host = new File(root, "peers.txt");
                try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(host), Global.INTERNAL_CHARSET))) {
                    output.write("localhost " + environments[0].getPort());
                    output.newLine();
                }
            }

            // 倉庫の初期化。
            final File storage = new File(root, "storage");
            storage.mkdirs();
            Assert.assertTrue(storage.exists());
            FileFunctions.deleteContents(storage);

            final Option option = new Option(("-root " + root.getPath() +
                    " -port " + Integer.toString(port) +
                    " -bbsPort " + Integer.toString(bbsPort) +
                    " -maintenanceInterval " + Long.toString(maintenanceInterval) +
                    " -operationTimeout " + Long.toString(operationTimeout) +
                    " -peerCapacity " + Integer.toString((int) Math.ceil(MathFunctions.log2(numOfPeers)))).split(" "));
            environments[i] = new Environment(option);

            covers[i] = new A(environments[i]);
            executor.submit(new CoverWrapper(covers[i]));

            // 起動待ちの遅延。
            Thread.sleep(100L);
        }

        // 安定待ち。
        waitStable(covers, maintenanceInterval);

        // 板の取得。
        final InetSocketAddress server = new InetSocketAddress("localhost", environments[0].getBbsPort());
        final String boardName = "test";
        Client.BbsBoard board = Client.getBoard(server, boardName);
        Assert.assertNotNull(board);
        Assert.assertEquals(0, board.getEntries().size());

        // スレの追加。
        final String title = "死ね死ねマン参上";
        final String author = "影のキリコ";
        final String mail = "";
        String message = "死ね×" + Long.toString(System.currentTimeMillis());
        Assert.assertTrue(Client.addThread(server, boardName, title, author, mail, message));

        // 板の再取得。
        board = Client.getBoard(server, boardName);
        Assert.assertNotNull(board);
        Assert.assertEquals(1, board.getEntries().size());
        Assert.assertEquals(title, board.getEntries().get(0).getTitle());

        // スレの取得。
        final String threadName = board.getEntries().get(0).getName().replaceFirst("\\.dat$", "");
        Client.BbsThread thread = Client.getThread(server, boardName, threadName);
        Assert.assertNotNull(thread);
        Assert.assertEquals(1, thread.getEntries().size());
        Assert.assertEquals(message, thread.getEntries().get(0).getMessage());

        // 書き込み。
        message = "1乙";
        Assert.assertTrue(Client.addComment(server, boardName, threadName, author, mail, message));

        // スレの再取得。
        thread = Client.getThread(server, boardName, threadName);
        Assert.assertNotNull(thread);
        Assert.assertEquals(2, thread.getEntries().size());
        Assert.assertEquals(message, thread.getEntries().get(1).getMessage());

        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        for (final A cover : covers) {
            cover.close();
        }
    }

    /**
     * 実際に運用してみる検査。
     * @throws Exception 異常
     */
    // @Test
    public void testManualOperation10_1() throws Exception {
        testManualOperation(10, 1);
    }

    /**
     * 実際に運用してみる検査。
     * @throws Exception 異常
     */
    // @Test
    public void testManualOperation10_10() throws Exception {
        testManualOperation(10, 10);
    }

    private static void testManualOperation(final int numOfPeers, final int numOfClients) throws Exception {
        final long operationTimeout = 500L;
        final long maintenanceInterval = Duration.SECOND;
        final long backupInterval = 5 * Duration.SECOND;

        final Environment[] environments = new Environment[numOfPeers];
        final A[] covers = new A[numOfPeers];

        final ExecutorService executor = Executors.newCachedThreadPool();

        for (int i = 0; i < numOfPeers; i++) {
            final File root = new File(ROOT, "manual" + Integer.toString(numOfPeers) + "-" + Integer.toString(numOfClients) + "-"
                    + String.format("%0" + Integer.toString((int) Math.ceil(Math.log10(numOfPeers))) + "d", i));
            final int port = FIRST_PORT + portOffset.getAndIncrement();
            final int bbsPort = (i == 0 ? 22_266 : FIRST_PORT + portOffset.getAndIncrement());
            (new File(root, "peers.txt")).delete();
            final File resource = new File(root, "resource");
            (new File(resource, "addressedPeers.dat")).delete();
            (new File(resource, "peers.dat")).delete();
            if (i != 0) {
                // 初期個体を追加。
                resource.mkdirs();
                Assert.assertTrue(resource.exists());
                final File host = new File(root, "peers.txt");
                try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(host), Global.INTERNAL_CHARSET))) {
                    output.write("localhost " + environments[0].getPort());
                    output.newLine();
                }
            }

            // 倉庫の初期化。
            final File storage = new File(root, "storage");
            storage.mkdirs();
            Assert.assertTrue(storage.exists());
            FileFunctions.deleteContents(storage);

            final Option option = new Option(("-root " + root.getPath() +
                    " -port " + Integer.toString(port) +
                    " -bbsPort " + Integer.toString(bbsPort) +
                    " -operationTimeout " + Long.toString(operationTimeout) +
                    " -maintenanceInterval " + Long.toString(maintenanceInterval) +
                    " -backupInterval " + Long.toString(backupInterval) +
                    " -peerCapacity " + Integer.toString((int) Math.ceil(MathFunctions.log2(numOfPeers))) +
                    " -gui " + (i == 0)).split(" "));
            environments[i] = new Environment(option);

            covers[i] = new A(environments[i]);
            executor.submit(new CoverWrapper(covers[i]));

            // 起動待ちの遅延。
            Thread.sleep(100L);
        }

        waitStable(covers, maintenanceInterval);
        System.out.println("通信網構築終了。");

        final long lifetime = 10 * Duration.MINUTE;
        final long interval = lifetime / 1_000;
        final long waitTime = 10 * Duration.MINUTE;
        final double writeRate = 0.5;
        final String boardName = "test";

        final List<Callable<Void>> clients = new ArrayList<>();
        for (int i = 0; i < numOfClients; i++) {
            final Environment environment = environments[i % environments.length];
            clients.add(TestClients.newSequential(new InetSocketAddress("localhost", environments[i % environments.length].getBbsPort()), boardName, interval,
                    environment.getMaintenanceInterval(), environment.getCacheDuration(), writeRate, i + " 番目の香具師"));
        }

        final List<Future<Void>> futures = new ArrayList<>();
        final ExecutorService clientExecutor = Executors.newFixedThreadPool(clients.size());
        for (final Callable<Void> client : clients) {
            futures.add(clientExecutor.submit(client));
            Thread.sleep(interval / numOfClients);
        }

        Thread.sleep(lifetime);

        clientExecutor.shutdownNow();
        Assert.assertTrue(clientExecutor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        System.out.println("クライアント活動停止。");

        // 実験待ち。
        Thread.sleep(waitTime);

        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        for (final A cover : covers) {
            cover.close();
        }
    }

}
