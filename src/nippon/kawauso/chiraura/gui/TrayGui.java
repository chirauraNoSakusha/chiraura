/**
 * 
 */
package nippon.kawauso.chiraura.gui;

import java.awt.AWTException;
import java.awt.Button;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Image;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.container.Pair;

/**
 * システムトレイで稼動情報を表示する感じ。
 * @author chirauraNoSakusha
 */
public final class TrayGui implements Gui {

    private static final Logger LOG = Logger.getLogger(TrayGui.class.getName());

    // 保持。
    private final BlockingQueue<GuiCommand> taskQueue;

    private final Image normalImage;
    private final Image warningImage;

    private final String rootPath;
    private final int bbsPort;

    private SystemTray tray;
    private TrayIcon icon;
    private Dialog suicideDialog;

    private Pair<InetSocketAddress, String> self;
    private boolean jceError;
    private boolean serverError;
    private int closePortWarning;
    private Pair<Long, Long> versionGapWarning;

    private String warnings;

    /**
     * 作成する。
     * @param rootPath 作業場の場所
     * @param bbsPort 2ch サーバのポート番号
     */
    public TrayGui(final String rootPath, final int bbsPort) {
        this.taskQueue = new LinkedBlockingQueue<>();

        this.rootPath = rootPath;
        this.bbsPort = bbsPort;

        if (SystemTray.isSupported()) {
            this.tray = SystemTray.getSystemTray();
            this.normalImage = IconImages.getNormalImage();
            this.warningImage = IconImages.getWarningImage();
        } else {
            this.tray = null;
            this.normalImage = null;
            this.warningImage = null;
        }
        this.icon = null;
        this.suicideDialog = null;

        this.self = null;
        this.jceError = false;
        this.serverError = false;
        this.closePortWarning = -1;
        this.versionGapWarning = null;

        this.warnings = null;

    }

    @Override
    public GuiCommand take() throws InterruptedException {
        return this.taskQueue.take();
    }

    @Override
    public synchronized void start(final ExecutorService executor) {
        if (this.tray == null) {
            LOG.log(Level.WARNING, "システムトレイが無いようです。");
            return;
        }

        final PopupMenu menu = new PopupMenu();

        /*
         * 俺の環境だとなぜか背景サイズが 24x24 になるし、透けないけど、
         * Windows だと大丈夫っぽいので気にしない。
         */
        this.icon = new TrayIcon(this.normalImage, "ちらしの裏", menu);

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
         * 右クリックのメニューから該当項目を選んだら、
         * 公開用個体情報をシステムのクリップボードにコピーする。
         */
        final MenuItem peerCopyItem = new MenuItem("公開用の個体情報をコピー");
        menu.add(peerCopyItem);
        peerCopyItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final String str;
                if (TrayGui.this.self != null) {
                    str = "^" + TrayGui.this.self.getSecond();
                } else {
                    str = "まだ個体情報が確定していません。";
                }
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), null);
            }
        });

        /*
         * 終了。
         * 右クリックのメニューから該当項目を選んだら、
         * 独立ウィンドウで確認して、確認できたらアプリを終了する。
         */
        this.suicideDialog = new Dialog((Frame) null, "本気で止めますか？");
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
        menu.add(suicideItem);
        suicideItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                TrayGui.this.suicideDialog.setVisible(true);
            }
        });

        try {
            this.tray.add(this.icon);
        } catch (final AWTException e) {
            LOG.log(Level.WARNING, "システムトレイの使用に失敗しました", e);
        }
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

        buff.append("2chポート:").append(Integer.toString(this.bbsPort)).append(", ").append(System.lineSeparator());

        buff.append("作業場:").append(this.rootPath).append(", ").append(System.lineSeparator());

        return buff.toString();
    }

    private String makeWarnings() { // synchronized から呼ぶ。
        final StringBuilder buff = new StringBuilder();

        if (this.jceError) {
            buff.append("JCEの制限が解除されていないようです").append(", ").append(System.lineSeparator());
        }

        if (this.serverError) {
            buff.append("サーバーを起動できません").append(", ").append(System.lineSeparator());
        }

        if (this.closePortWarning >= 0) {
            buff.append("ポート").append(Integer.toString(this.closePortWarning)).append("が開いていないかもしれません").append(", ").append(System.lineSeparator());
        }

        if (this.versionGapWarning != null) {
            buff.append("この個体より ");
            if (this.versionGapWarning.getFirst() > 0) {
                if (this.versionGapWarning.getSecond() > 0) {
                    buff.append(Long.toString(this.versionGapWarning.getFirst())).append(" 段階と ")
                            .append(Long.toString(this.versionGapWarning.getSecond())).append(" 歩");
                } else {
                    buff.append(Long.toString(this.versionGapWarning.getFirst())).append(" 段階");
                }
            } else {
                buff.append(Long.toString(this.versionGapWarning.getSecond())).append(" 歩");
            }
            buff.append("だけ新しい個体がいるようです, ").append(System.lineSeparator());
        }

        return buff.toString();
    }

    private void printWarnings() { // synchronized から呼ぶ。
        final String newWarnings = makeWarnings();
        if (newWarnings.equals(this.warnings)) {
            return;
        }

        this.warnings = newWarnings;
        if (this.warnings.length() > 0) {
            this.icon.setImage(this.warningImage);
            this.icon.displayMessage("警告", newWarnings, TrayIcon.MessageType.WARNING);
        } else {
            this.icon.setImage(this.normalImage);
        }
    }

    @Override
    public synchronized void setSelf(final InetSocketAddress peer, final String publicString) {
        if (this.tray == null) {
            return;
        } else if (this.self == null || !peer.equals(this.self.getFirst())) {
            this.self = new Pair<>(peer, publicString);
        }
    }

    @Override
    public synchronized void displayJceError() {
        if (this.tray == null || this.jceError) {
            return;
        } else {
            this.jceError = true;
            printWarnings();
        }
    }

    @Override
    public synchronized void displayServerError() {
        if (this.tray == null || this.serverError) {
            return;
        } else {
            this.serverError = true;
            printWarnings();
        }
    }

    @Override
    public synchronized void displayClosePortWarning(final int port) {
        if (this.tray == null || port == this.closePortWarning) {
            return;
        } else {
            this.closePortWarning = port;
            printWarnings();
        }
    }

    @Override
    public synchronized void displayVersionGapWarning(final long majorGap, final long minorGap) {
        if (this.tray == null) {
            return;
        } else if (this.versionGapWarning == null || this.versionGapWarning.getFirst() < majorGap
                || (this.versionGapWarning.getFirst() == majorGap && this.versionGapWarning.getSecond() < minorGap)) {
            this.versionGapWarning = new Pair<>(majorGap, minorGap);
            printWarnings();
        }
    }

}
