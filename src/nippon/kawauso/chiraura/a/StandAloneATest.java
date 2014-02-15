/**
 * 
 */
package nippon.kawauso.chiraura.a;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.lib.process.Reporter;

import org.junit.Assert;

/**
 * @author chirauraNoSakusha
 */
public final class StandAloneATest {

    private static final File root = new File((new File(System.getProperty("user.dir"))).getPath() + File.separator + "tmp" + File.separator + "test"
            + File.separator + StandAloneATest.class.getSimpleName());

    /**
     * 実際に運用してみる検査。
     * @throws Exception 異常
     */
    // @Test
    public void testManualOperation() throws Exception {
        LoggingFunctions.startLogging();

        final int numOfClients = 10;
        final File dir = new File(root, "backup" + Integer.toString(numOfClients));

        final Option option = new Option(("-root " + dir.getPath()).split(" "));
        final Environment environment = new Environment(option);
        final StandAloneA cover = new StandAloneA(environment);

        final long lifetime = 3 * Duration.MINUTE;
        final long interval = lifetime / 1_000;
        final long waitTime = 10 * Duration.MINUTE;

        // final long lifetime = 1 * Duration.MINUTE;
        // final long interval = lifetime / 100;
        // final long waitTime = 1 * Duration.MINUTE;

        final double writeRate = 0.5;
        final String boardName = "test";
        final List<Callable<Void>> clients = new ArrayList<>();
        for (int i = 0; i < numOfClients; i++) {
            clients.add(TestClients.newSequential(new InetSocketAddress("localhost", environment.getBbsPort()), boardName, interval,
                    environment.getMaintenanceInterval(), environment.getCacheDuration(), writeRate, i + " 番目の香具師"));
        }

        final ExecutorService executor = Executors.newCachedThreadPool();
        executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws Exception {
                cover.execute();
                return null;
            }
        });

        // 2ch サーバの稼動待ち。
        Thread.sleep(Duration.SECOND);

        final ExecutorService clientExecutor = Executors.newFixedThreadPool(clients.size());
        for (final Callable<Void> client : clients) {
            clientExecutor.submit(client);
            Thread.sleep(interval / numOfClients);
        }

        Thread.sleep(lifetime);

        clientExecutor.shutdownNow();
        Assert.assertTrue(clientExecutor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));
        System.out.println("クライアント活動停止。");

        // 実験待ち。
        LoggingFunctions.startDebugLogging();
        Thread.sleep(waitTime);

        executor.shutdownNow();
        Assert.assertTrue(executor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS));

        cover.close();
        LogInitializer.reset();
    }

}
