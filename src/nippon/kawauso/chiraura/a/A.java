/**
 * 
 */
package nippon.kawauso.chiraura.a;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.bbs.BasicBbs;
import nippon.kawauso.chiraura.bbs.Bbs;
import nippon.kawauso.chiraura.bbs.BoardChunkConverter;
import nippon.kawauso.chiraura.closet.p2p.P2pCloset;
import nippon.kawauso.chiraura.gui.Gui;
import nippon.kawauso.chiraura.gui.TrayGui;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;
import nippon.kawauso.chiraura.network.AddressedPeer;
import nippon.kawauso.chiraura.storage.Storage;
import nippon.kawauso.chiraura.storage.Storages;

/**
 * @author chirauraNoSakusha
 */
public final class A implements AutoCloseable {

    static {
        LogInitializer.init();
    }

    private static final Logger LOG = Logger.getLogger(A.class.getName());

    private final Environment environment;
    private final P2pCloset closet;
    private final Bbs bbs;
    private final Gui gui;

    private final CountDownLatch stopper;

    private final AtomicBoolean shutdownStarted;

    A(final Environment environment) throws IOException {
        if (environment == null) {
            throw new IllegalArgumentException("Null environment.");
        }

        this.environment = environment;

        final KeyPair id = environment.loadId();
        this.closet = new P2pCloset(
                (new P2pCloset.Parameters(environment.getStorageRoot(), id, environment.getPort(), environment.getExecutor()))
                        .setChunkSizeLimit(environment.getChunkSizeLimit())
                        .setStorageDirectoryBitSize(environment.getStorageDirectoryBitSize())
                        .setChunkCacheCapacity(environment.getChunkCacheCapacity())
                        .setIndexCacheCapacity(environment.getIndexCacheCapacity())
                        .setRangeCacheCapacity(environment.getRangeCacheCapacity())
                        .setPeerCapacity(environment.getPeerCapacity())
                        .setMaintenanceInterval(environment.getMaintenanceInterval())
                        .setSleepTime(environment.getSleepTime())
                        .setBackupInterval(environment.getBackupInterval())
                        .setConnectionTimeout(environment.getConnectionTimeout())
                        .setReceiveBufferSize(environment.getReceiveBufferSize())
                        .setSendBufferSize(environment.getSendBuffereSize())
                        .setPublicKeyLifetime(environment.getPublicKeyLifetime())
                        .setCommonKeyLifetime(environment.getCommonKeyLifetime())
                        .setPortIgnore(environment.getPortIgnore())
                        .setConnectionLimit(environment.getConnectionLimit())
                        .setTrafficDuration(environment.getTrafficDuration())
                        .setTrafficSizeLimit(environment.getTrafficSizeLimit())
                        .setTrafficCountLimit(environment.getTrafficCountLimit())
                        .setTrafficPenalty(environment.getTrafficPenalty())
                        .setBlacklistCapacity(environment.getBlacklistCapacity())
                        .setBlacklistTimeout(environment.getBlacklistTimeout())
                        .setPotCapacity(environment.getPotCapacity())
                        .setOperationTimeout(environment.getOperationTimeout())
                        .setMessageSizeLimit(environment.getMessageSizeLimit())
                        .setCacheLogCapacity(environment.getCacheLogCapacity())
                        .setCacheDuration(environment.getCacheDuration())
                        .setAddressedPeers(environment.loadAddressedPeers())
                        .setPeers(environment.loadPeers())
                        .setCalculator(environment.getAddressCalculator())
                        .setActiveAddressLogCapacity(environment.getActiveAddressLogCapacity())
                        .setActiveAddressDuration(environment.getActiveAddressDuration())
                );

        this.bbs = new BasicBbs(environment.getBbsPort(), environment.getBbsConnectionTimeout(), environment.getBbsInternalTimeout(), this.closet,
                environment.getBbsUpdateThreshold(), environment.loadBbsMenu());

        if (environment.getGui()) {
            this.gui = new TrayGui(environment.getRootPath(), environment.getBbsPort(), environment.getGuiBootDuration(),
                    environment.getGuiMaxDelay(), environment.getGuiInterval());
        } else {
            this.gui = null;
        }

        this.stopper = new CountDownLatch(1);

        this.shutdownStarted = new AtomicBoolean(false);

        if (Global.isDebug()) {
            LOG.log(Level.WARNING, "開発形態です。");
        }

        LOG.log(Level.FINEST, "私の論理位置は {0} です。", this.environment.getAddressCalculator().calculate(id.getPublic()));
    }

    KeyPair getId() {
        return this.closet.getId();
    }

    List<AddressedPeer> getPeers() {
        return this.closet.getPeers();
    }

    private List<InetSocketAddress> getBackupPeers() {
        return this.closet.getBackupPeers();
    }

    List<AddressedPeer> getImportantPeers() {
        return this.closet.getImportantPeers();
    }

    void execute() {
        /*
         * ログの設定はやらないので、並列に呼んでもきっと大丈夫。
         */

        if (this.gui != null) {
            this.gui.start(this.environment.getExecutor());
        }
        this.closet.start(this.environment.getExecutor());
        this.bbs.start(this.environment.getExecutor());

        final Boss boss = new Boss(this.closet, this.gui, this.stopper, this.environment, this.environment.getExecutor());
        this.environment.getExecutor().submit(boss);

        if (P2pCloset.isLimitedJce()) {
            LOG.log(Level.SEVERE, "JCEの制限が解除されていないようです。");
            if (this.gui != null) {
                this.gui.displayJceError();
            }
        }

        try {
            this.stopper.await();
        } catch (final InterruptedException e) {
            LOG.log(Level.FINEST, "終了信号を受け取りました。");
        }

        LOG.log(Level.FINEST, "終了処理に入ります。");
        this.environment.getExecutor().shutdownNow();

        // Windows ではなぜか失敗するのでやらない。
        // if (this.gui != null) {
        // if (this.gui.printMessage("終了処理に入ります。")) {
        // try {
        // Thread.sleep(3 * Duration.SECOND);
        // } catch (final InterruptedException ignored) {
        // }
        // }
        // }

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
        if (this.gui != null && !this.shutdownStarted.get()) {
            // システムの終了が始まっている場合、GUI は勝手に死ぬのでやらない。
            // それどころか Windows ではやろうとすると途中で止まる。
            this.gui.close();
        }
        this.environment.storeAddressedPeers(getPeers());
        this.environment.storePeers(getBackupPeers());
    }

    /**
     * 起動装置。
     * @param args オプション
     * @throws IOException 設定ファイルやらの読み書き異常
     * @throws FileNotFoundException 指定の設定ファイルが無かった場合
     */
    public static void main(final String[] args) throws FileNotFoundException, IOException {
        final CountDownLatch terminatorStopper = new CountDownLatch(1);

        // 終了判定に使う。
        ExecutorService executor = null;
        boolean gui = false;

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
            preprocess(environment);

            final A instance = new A(environment);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    LOG.log(Level.FINEST, "システムが終了されます。");
                    instance.shutdownStarted.set(true);
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
            gui = environment.getGui();

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

        // 俺の環境では、トレイアイコンの PopupMenu を表示すると終了できなくなる。
        // それに対する暫定的な処置。
        // 気持ち悪いが、本来、正常終了している位置だから特に問題無いはず。
        if (gui) {
            System.exit(0);
        }
    }

    static void preprocess(final Environment env) throws IOException, InterruptedException, MyRuleException {
        // 旧板データを新板データに変換する。
        final Storage storage = Storages.newInstance(env.getStorageRoot(), env.getChunkSizeLimit(), env.getStorageDirectoryBitSize(),
                env.getChunkCacheCapacity(), env.getIndexCacheCapacity(), env.getRangeCacheCapacity());
        BoardChunkConverter.convert(storage);
        storage.close();
    }

}
