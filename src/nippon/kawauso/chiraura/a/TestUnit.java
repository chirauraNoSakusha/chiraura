/**
 * 
 */
package nippon.kawauso.chiraura.a;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * 試験のための起点
 * @author chirauraNoSakusha
 */
final class TestUnit {

    static {
        LogInitializer.init();
    }

    private static final Logger LOG = Logger.getLogger(TestUnit.class.getName());

    private static final String BOARD_NAME = "test";

    public static void main(final String[] args) throws FileNotFoundException, IOException, InterruptedException {
        LoggingFunctions.startLogging();

        /*
         * オプションの形式:
         * lifetime clientType clientOptions... chirauraOptions...
         */

        final Queue<String> argList = new LinkedList<>(Arrays.asList(args));

        if (argList.size() < 2) {
            throw new IllegalArgumentException("No required options: lifetime clientType.");
        }

        final long lifetime = Long.parseLong(argList.poll());
        if (lifetime < 0) {
            throw new IllegalArgumentException("Negative lifetime ( " + lifetime + " ).");
        }

        final String clientLabel = argList.poll();

        final Queue<String> clientOptions = new LinkedList<>();
        if (clientLabel.equals("Dummy")) {
            /*
             * clientOptions... は無し。
             */
        } else if (clientLabel.equals("Sequential")) {
            /*
             * clientOptions... の形式:
             * interval writeRate author
             */
            if (argList.size() < 3) {
                throw new IllegalArgumentException("No required options for Sequential: interval writeRate author.");
            }
            for (int i = 0; i < 3; i++) {
                clientOptions.add(argList.poll());
            }
        } else if (clientLabel.equals("Rom")) {
            /*
             * clientOptions... の形式:
             * interval boardRate
             */
            if (argList.size() < 2) {
                throw new IllegalArgumentException("No required options for Rom: interval boardRate.");
            }
            for (int i = 0; i < 2; i++) {
                clientOptions.add(argList.poll());
            }
        } else {
            throw new IllegalArgumentException("Invalid client label ( " + clientLabel + " ).");
        }

        final Option option = new Option(argList.toArray(new String[0]));
        final Environment environment = new Environment(option);

        final Callable<Void> client;
        if (clientLabel.equals("Dummy")) {
            /*
             * clientOptions... は無し。
             */
            client = new Callable<Void>() {
                @Override
                public Void call() {
                    return null;
                }
            };
        } else if (clientLabel.equals("Sequential")) {
            /*
             * clientOptions... の形式:
             * interval writeRate author
             */
            if (argList.size() < 3) {
                throw new IllegalArgumentException("No required options for Sequential: interval writeRate author.");
            }

            final long interval = Long.parseLong(clientOptions.poll());
            if (lifetime < 0) {
                throw new IllegalArgumentException("Negative interval ( " + interval + " ).");
            }
            final double writeRate = Double.parseDouble(clientOptions.poll());
            if (writeRate < 0 || 1 < writeRate) {
                throw new IllegalArgumentException("Invalid write rate ( " + writeRate + " ).");
            }
            final String author = clientOptions.poll();

            client = TestClients.newSequential(new InetSocketAddress("localhost", environment.getBbsPort()), BOARD_NAME, interval,
                    environment.getMaintenanceInterval(), environment.getCacheDuration(), writeRate, author);
        } else if (clientLabel.equals("Rom")) {
            /*
             * clientOptions... の形式:
             * interval boardRate
             */
            if (argList.size() < 2) {
                throw new IllegalArgumentException("No required options for Rom: interval boardRate.");
            }

            final long interval = Long.parseLong(clientOptions.poll());
            if (lifetime < 0) {
                throw new IllegalArgumentException("Negative interval ( " + interval + " ).");
            }
            final double boardRate = Double.parseDouble(clientOptions.poll());
            if (boardRate < 0 || 1 < boardRate) {
                throw new IllegalArgumentException("Invalid board rate ( " + boardRate + " ).");
            }

            client = TestClients.newRom(new InetSocketAddress("localhost", environment.getBbsPort()), BOARD_NAME, interval, boardRate);
        } else {
            throw new IllegalArgumentException("Invalid client label ( " + clientLabel + " ).");
        }

        // ちらしの裏の起動。
        final ExecutorService chirauraExecutor = Executors.newSingleThreadExecutor();
        final ExecutorService clientExecutor = Executors.newSingleThreadExecutor();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                clientExecutor.shutdownNow();
                try {
                    if (!clientExecutor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS)) {
                        LOG.log(Level.WARNING, clientLabel + " をうまく終了させられませんでした。");
                    }
                    chirauraExecutor.shutdownNow();
                    if (!chirauraExecutor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS)) {
                        LOG.log(Level.WARNING, "ちらしの裏をうまく終了させられませんでした。");
                    }
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        chirauraExecutor.submit(new Reporter<Void>(Level.SEVERE) {
            @Override
            protected Void subCall() throws Exception {
                A.main(argList.toArray(new String[0]));
                return null;
            }
        });

        // 同期待ち。
        Thread.sleep(2 * environment.getMaintenanceInterval());

        // 2ch クライアントの起動
        clientExecutor.submit(client);

        Thread.sleep(lifetime);

        // 2ch クライアントの終了。
        clientExecutor.shutdownNow();
        if (!clientExecutor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS)) {
            LOG.log(Level.WARNING, clientLabel + " をうまく終了させられませんでした。");
        }

        // ちらしの裏の終了。
        chirauraExecutor.shutdownNow();
        if (!chirauraExecutor.awaitTermination(Duration.SECOND, TimeUnit.MILLISECONDS)) {
            LOG.log(Level.WARNING, "ちらしの裏をうまく終了させられませんでした。");
        }
    }

}
