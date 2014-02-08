/**
 * 
 */
package nippon.kawauso.chiraura.a;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.bbs.BasicBbs;
import nippon.kawauso.chiraura.bbs.Bbs;
import nippon.kawauso.chiraura.closet.Closet;
import nippon.kawauso.chiraura.closet.alone.StandAloneCloset;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;

/**
 * @author chirauraNoSakusha
 */
public final class StandAloneA implements AutoCloseable {

    static {
        LogInitializer.init();
    }

    private static final Logger LOG = Logger.getLogger(StandAloneA.class.getName());

    private final Environment environment;
    private final Closet closet;
    private final Bbs bbs;

    private final CountDownLatch stopper;

    StandAloneA(final Environment environment) {
        this.environment = environment;
        this.closet = new StandAloneCloset(environment.getStorageRoot(), environment.getChunkSizeLimit(), environment.getStorageDirectoryBitSize(),
                environment.getChunkCacheCapacity(), environment.getIndexCacheCapacity(), environment.getRangeCacheCapacity());
        this.bbs = new BasicBbs(environment.getBbsPort(), environment.getBbsConnectionTimeout(), environment.getBbsInternalTimeout(), this.closet,
                environment.getBbsUpdateThreshold(), environment.getDefaultNames());
        this.stopper = new CountDownLatch(1);
    }

    void execute() {
        /*
         * ログの設定はやらないので、並列に呼んでもきっと大丈夫。
         */

        this.closet.start(this.environment.getExecutor());
        this.bbs.start(this.environment.getExecutor());

        try {
            this.stopper.await();
        } catch (final InterruptedException e) {
            LOG.log(Level.FINEST, "終了信号を受け取りました。");
        }

        LOG.log(Level.FINEST, "終了処理に入ります。");
        this.environment.getExecutor().shutdownNow();
        try {
            if (!this.environment.getExecutor().awaitTermination(this.environment.getShutdownTimeout(), TimeUnit.MILLISECONDS)) {
                LOG.log(Level.SEVERE, "{0} ミリ秒以内にプロセスが終了しませんでした。", this.environment.getShutdownTimeout());
            }
        } catch (final InterruptedException e) {
            LOG.log(Level.SEVERE, "プロセスの終了を待っている間に急かされたので、待つのを止めます。");
        }
    }

    @Override
    public void close() throws MyRuleException, InterruptedException, IOException {
        this.bbs.close();
        this.closet.close();
    }

    /**
     * 起動スイッチ。
     * @param args オプション
     * @throws IOException 設定ファイルやらの読み書き異常
     * @throws FileNotFoundException 指定の設定ファイルが無かった場合
     */
    public static void main(final String[] args) throws FileNotFoundException, IOException {
        final CountDownLatch terminatorStopper = new CountDownLatch(1);

        ExecutorService executor = null;

        try {
            LoggingFunctions.startLogging();

            final Option option = new Option(args);

            if (Boolean.parseBoolean(option.get(Option.Item.help))) {
                // ヘルプを表示して終わり。
                System.out.println(Option.toHelpString());
                return;
            }

            final Environment environment = new Environment(option);
            environment.startLogging();
            option.afterStartLogging();

            LOG.log(Level.CONFIG, "以下の設定が使用されます: " + System.lineSeparator() + option.toCommandlineString());

            // TODO 暫定的処置なので、そのうち消すように。
            A.preprocess(environment);

            final StandAloneA instance = new StandAloneA(environment);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    instance.stopper.countDown();
                    try {
                        if (!terminatorStopper.await(environment.getShutdownTimeout(), TimeUnit.MILLISECONDS)) {
                            LOG.log(Level.SEVERE, "{0} ミリ秒以内に終処理が完了しませんでした。", environment.getShutdownTimeout());
                        }
                    } catch (final InterruptedException e) {
                        // こんなところにまで終了信号が来るときは諦めて死ぬ。
                        LOG.log(Level.SEVERE, "終処理が完了しませんでした。");
                    }
                }
            });

            // 異常時の強制終了のために保存。
            executor = environment.getExecutor();

            instance.execute();

            try {
                instance.close();
            } catch (MyRuleException | InterruptedException | IOException e) {
                LOG.log(Level.SEVERE, "データの保管に失敗したかもしれません。", e);
            }
        } catch (final Throwable e) {
            LOG.log(Level.SEVERE, "予期せぬ異常が発生しました", e);
        } finally {
            try {
                if (executor != null && !executor.isTerminated()) {
                    LOG.log(Level.SEVERE, "強制終了します。");
                    System.exit(1);
                } else {
                    LogInitializer.reset();
                }
            } finally {
                try {
                    terminatorStopper.countDown();
                } finally {
                    LogInitializer.reset();
                }
            }
        }
    }

}
