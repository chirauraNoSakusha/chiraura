/**
 * 
 */
package nippon.kawauso.chiraura.gui;

import java.awt.AWTException;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JDialog;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.container.Pair;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * システムトレイで稼動情報を表示する感じ。
 * @author chirauraNoSakusha
 */
public final class TrayGui implements Gui {

    private static final Logger LOG = Logger.getLogger(TrayGui.class.getName());

    // 参照。
    private final String rootPath;
    private final int bbsPort;

    private final long bootDuration;
    private ExecutorService executor;

    // 保持。
    private final BlockingQueue<GuiCommand> taskQueue;

    private final Image normalImage;
    private final Image warningImage;
    private final Image severeImage;

    private SystemTray tray;
    private TrayIcon icon;
    private JDialog suicideDialog;

    private Pair<InetSocketAddress, String> self;
    private boolean jceError;
    private boolean serverError;
    private int closePortWarning;
    private Pair<Long, Long> versionGapWarning;

    private String warnings;

    private final long delay;
    private long boot;
    private long start;
    private Pair<Long, Long> versionGapWarningBuffer;

    private long interval;
    private final BlockingQueue<Long> intervalCahnger;

    /**
     * 作成する。
     * @param rootPath 作業場の場所
     * @param bbsPort 2ch サーバのポート番号
     * @param bootDuration 起動したばかりとみなす時間
     * @param maxDelay 更新警告の最大遅延時間 (ミリ秒)
     * @param initialInterval 警告表示を繰り返す間隔 (ミリ秒)
     */
    public TrayGui(final String rootPath, final int bbsPort, final long bootDuration, final long maxDelay, final long initialInterval) {
        if (bootDuration < 0) {
            throw new IllegalArgumentException("Negative boot duration ( " + bootDuration + " )");
        } else if (maxDelay <= 0) {
            throw new IllegalArgumentException("Too small max delay ( " + maxDelay + " )");
        } else if (initialInterval < 0) {
            throw new IllegalArgumentException("Negative initial interval ( " + initialInterval + " )");
        }
        this.rootPath = rootPath;
        this.bbsPort = bbsPort;

        this.bootDuration = bootDuration;
        this.executor = null;

        this.taskQueue = new LinkedBlockingQueue<>();

        if (SystemTray.isSupported()) {
            this.tray = SystemTray.getSystemTray();
            this.normalImage = IconImages.getNormalImage();
            this.warningImage = IconImages.getWarningImage();
            this.severeImage = IconImages.getSevereImage();
        } else {
            this.tray = null;
            this.normalImage = null;
            this.warningImage = null;
            this.severeImage = null;
        }
        this.icon = null;
        this.suicideDialog = null;

        this.self = null;
        this.jceError = false;
        this.serverError = false;
        this.closePortWarning = -1;
        this.versionGapWarning = null;

        this.warnings = "";

        this.delay = ThreadLocalRandom.current().nextLong(maxDelay);
        this.boot = -1;
        this.start = -1;
        this.versionGapWarningBuffer = null;

        this.interval = initialInterval;
        this.intervalCahnger = new LinkedBlockingQueue<>();
    }

    private synchronized Pair<InetSocketAddress, String> getSelf() {
        return this.self;
    }

    private synchronized long getInterval() {
        return this.interval;
    }

    private synchronized void setInterval(final long interval) {
        this.interval = interval;
    }

    @Override
    public GuiCommand take() throws InterruptedException {
        return this.taskQueue.take();
    }

    @Override
    public synchronized void start(final ExecutorService executor0) {
        if (this.tray == null) {
            LOG.log(Level.WARNING, "システムトレイが無いようです。");
            return;
        }

        this.executor = executor0;

        final PopupMenu popup = new PopupMenu();

        /*
         * 俺の環境だとなぜか背景サイズが 24x24 になるし、透けないけど、
         * Windows だと大丈夫っぽいので気にしない。
         */
        this.icon = new TrayIcon(this.normalImage, "ちらしの裏", popup);

        /*
         * 個体情報の表示。
         * クリックしたら、個体情報を表示する。
         */
        this.icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                TrayGui.this.icon.displayMessage("個体情報", makePeerInfo(), TrayIcon.MessageType.NONE);
            }
        });

        /*
         * 公開用個体情報のコピー。
         * 右クリックのポップアップメニューから選ぶ。
         * 公開用個体情報をシステムのクリップボードにコピーする。
         */
        final MenuItem peerCopyItem = new MenuItem("公開用の個体情報をコピー");
        peerCopyItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Pair<InetSocketAddress, String> curSelf = getSelf();
                final String str;
                if (curSelf != null) {
                    str = "^" + curSelf.getSecond();
                } else {
                    str = "まだ個体情報が確定していません。";
                }
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), null);
            }
        });
        popup.add(peerCopyItem);

        /*
         * 警告間隔の変更。
         * 右クリックのポップアップメニューから選ぶ。
         * 更新間隔一覧のポップアップメニューを開く。
         */
        final Menu intervalMenu = new Menu("警告を表示する間隔");
        for (final Pair<String, Long> entry : Arrays.asList(
                new Pair<>("5 分", 5 * Duration.MINUTE),
                new Pair<>("10 分", 10 * Duration.MINUTE),
                new Pair<>("30 分", 30 * Duration.MINUTE),
                new Pair<>("1 時間", Duration.HOUR),
                new Pair<>("2 時間", 2 * Duration.HOUR),
                new Pair<>("5 時間", 5 * Duration.HOUR),
                new Pair<>("10 時間", 10 * Duration.HOUR),
                new Pair<>("1 日", Duration.DAY),
                new Pair<>("定期報告しない", 0L)
                )) {
            final MenuItem intervalItem = new MenuItem(entry.getFirst());
            intervalItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    try {
                        TrayGui.this.intervalCahnger.put(entry.getSecond());
                    } catch (final InterruptedException shutdown) {
                    }
                }
            });
            intervalMenu.add(intervalItem);
        }
        popup.add(intervalMenu);

        popup.addSeparator();

        /*
         * 終了。
         * 右クリックのメニューから該当項目を選んだら、
         * 独立ウィンドウで確認して、確認できたらアプリを終了する。
         */
        this.suicideDialog = new JDialog((Frame) null, "本気で止めますか？");
        this.suicideDialog.setMinimumSize(new Dimension(180, 60));
        this.suicideDialog.setLocationRelativeTo(null);

        // 閉じたら消える。
        this.suicideDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                TrayGui.this.suicideDialog.setVisible(false);
            }
        });
        // 良くある見た目。
        this.suicideDialog.setLayout(new FlowLayout());
        final Button suicideButton = new Button("はい");
        suicideButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                ConcurrentFunctions.completePut(new ShutdownCommand(), TrayGui.this.taskQueue);
                TrayGui.this.suicideDialog.setVisible(false);
            }
        });
        this.suicideDialog.add(suicideButton);

        final MenuItem suicideItem = new MenuItem("終了");
        suicideItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                TrayGui.this.suicideDialog.setVisible(true);
            }
        });
        popup.add(suicideItem);

        try {
            this.tray.add(this.icon);
        } catch (final AWTException e) {
            LOG.log(Level.WARNING, "システムトレイの使用に失敗しました", e);
            close();
            return;
        }

        // 定期報告用。
        this.executor.submit(new Reporter<Void>(Level.WARNING) {
            @Override
            protected Void subCall() throws InterruptedException {
                long curInterval = getInterval();
                while (true) {
                    final Long buff;
                    if (curInterval <= 0) {
                        buff = TrayGui.this.intervalCahnger.take();
                    } else {
                        buff = TrayGui.this.intervalCahnger.poll(curInterval, TimeUnit.MILLISECONDS);
                    }

                    if (buff == null) {
                        continue;
                    }

                    final long nextInterval = buff;
                    if (nextInterval != curInterval) {
                        curInterval = nextInterval;
                        setInterval(nextInterval);
                        LOG.log(Level.FINEST, "報告間隔を {0} ミリ秒に変更しました。", nextInterval);
                    }
                    printWarnings();
                }
            }
        });
    }

    @Override
    public synchronized void close() {
        if (this.tray == null) {
            return;
        }

        if (this.icon != null) {
            this.tray.remove(this.icon);
            this.normalImage.flush();
            this.warningImage.flush();
            LOG.log(Level.FINEST, "トレイアイコンを削除しました。");
        }

        if (this.suicideDialog != null) {
            this.suicideDialog.dispose();
            LOG.log(Level.FINEST, "終了ダイアログを破棄しました。");
        }

        this.tray = null;
    }

    private synchronized String makePeerInfo() {
        final StringBuilder buff = new StringBuilder();

        if (this.warnings != null) {
            buff.append(this.warnings);
        }

        if (this.self != null) {
            buff.append(this.self.getFirst()).append(", ");
        } else {
            buff.append("ぼっち, ");
        }
        buff.append(System.lineSeparator());

        buff.append("2chポート:").append(this.bbsPort).append(", ").append(System.lineSeparator());

        buff.append("作業場:").append(this.rootPath).append(", ").append(System.lineSeparator());

        if (this.interval <= 0) {
            buff.append("定期報告しない");
        } else {
            buff.append("定期報告間隔:").append(Duration.toString(this.interval));
        }

        return buff.toString();
    }

    private synchronized String makeWarnings() {
        final StringBuilder buff = new StringBuilder();

        if (this.jceError) {
            buff.append("JCEの制限が解除されていないようです").append(", ").append(System.lineSeparator());
        }

        if (this.serverError) {
            buff.append("サーバーを起動できません").append(", ").append(System.lineSeparator());
        }

        if (this.closePortWarning >= 0) {
            buff.append("ポート").append(this.closePortWarning).append("が開いていないかもしれません").append(", ").append(System.lineSeparator());
        }

        if (this.versionGapWarning != null) {
            buff.append("この個体より ");
            if (this.versionGapWarning.getFirst() > 0) {
                if (this.versionGapWarning.getSecond() > 0) {
                    buff.append(this.versionGapWarning.getFirst()).append(" 段階と ").append(this.versionGapWarning.getSecond()).append(" 歩");
                } else {
                    buff.append(this.versionGapWarning.getFirst()).append(" 段階");
                }
            } else {
                buff.append(this.versionGapWarning.getSecond()).append(" 歩");
            }
            buff.append("だけ新しい個体がいるようです, ").append(System.lineSeparator());
        }

        return buff.toString();
    }

    private synchronized void updateWarnings() {
        final String newWarnings = makeWarnings();
        if (newWarnings.equals(this.warnings)) {
            return;
        }

        this.warnings = newWarnings;
        if (this.warnings.length() > 0) {
            if (this.jceError || this.serverError) {
                this.icon.setImage(this.severeImage);
            } else {
                this.icon.setImage(this.warningImage);
            }
        } else {
            this.icon.setImage(this.normalImage);
        }
    }

    private synchronized void printWarnings() {
        if (this.warnings.length() > 0) {
            if (this.jceError || this.serverError) {
                this.icon.displayMessage("異常", this.warnings, TrayIcon.MessageType.ERROR);
            } else {
                this.icon.displayMessage("警告", this.warnings, TrayIcon.MessageType.WARNING);
            }
        }
    }

    @Override
    public synchronized void setSelf(final InetSocketAddress peer, final String publicString) {
        if (this.tray == null) {
            return;
        } else if (this.self == null || !peer.equals(this.self.getFirst())) {
            if (this.boot < 0) {
                this.boot = System.currentTimeMillis();
            }
            this.self = new Pair<>(peer, publicString);
        }
    }

    @Override
    public synchronized void displayJceError() {
        if (this.tray == null || this.jceError) {
            return;
        } else {
            this.jceError = true;
            updateWarnings();
            printWarnings();
        }
    }

    @Override
    public synchronized void displayServerError() {
        if (this.tray == null || this.serverError) {
            return;
        } else {
            this.serverError = true;
            updateWarnings();
            printWarnings();
        }
    }

    @Override
    public synchronized void displayClosePortWarning(final int port) {
        if (this.tray == null || port == this.closePortWarning) {
            return;
        } else {
            this.closePortWarning = port;
            updateWarnings();
            printWarnings();
        }
    }

    @Override
    public synchronized void displayVersionGapWarning(final long majorGap, final long minorGap) {
        if (this.tray == null) {
            return;
        } else if (this.versionGapWarningBuffer == null || this.versionGapWarningBuffer.getFirst() < majorGap
                || (this.versionGapWarningBuffer.getFirst() == majorGap && this.versionGapWarningBuffer.getSecond() < minorGap)) {
            /*
             * 以下の場合は直ぐに警告。
             * - ネットワークに入ったばかり。
             * そうでなければ、遅延警告。
             * 一斉終了によるネットワーク崩壊を防ぐため。
             */

            this.versionGapWarningBuffer = new Pair<>(majorGap, minorGap);

            final long cur = System.currentTimeMillis();
            if (this.boot < 0 || cur <= this.boot + this.bootDuration) {
                // ネットワークに入ったばかり。
                this.versionGapWarning = this.versionGapWarningBuffer;
                updateWarnings();
                printWarnings();
            } else if (this.start < 0) {
                // ネットワークに入ってから時間が経っていて、遅延が始まっていない。
                this.start = cur;
                LOG.log(Level.FINEST, "{0} ミリ秒お待ちください。", this.delay);
                this.executor.submit(new Reporter<Void>(Level.WARNING) {
                    @Override
                    protected Void subCall() throws InterruptedException {
                        Thread.sleep(TrayGui.this.delay);
                        synchronized (TrayGui.this) {
                            TrayGui.this.versionGapWarning = TrayGui.this.versionGapWarningBuffer;
                            updateWarnings();
                            printWarnings();
                        }
                        return null;
                    }
                });
            } else if (this.start + this.delay < cur) {
                // 既に遅延が終わっている。
                this.versionGapWarning = this.versionGapWarningBuffer;
                updateWarnings();
                printWarnings();
            } else {
                // 遅延中。
            }
        }
    }

}
