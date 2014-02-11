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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;

/**
 * システムトレイで稼動情報を表示する感じ。
 * @author chirauraNoSakusha
 */
public final class TrayGui implements Gui {

    private static final Logger LOG = Logger.getLogger(TrayGui.class.getName());

    // 保持。
    private final SystemTray tray;

    private final BlockingQueue<GuiCommand> taskQueue;

    private TrayIcon icon;
    private Dialog suicideDialog;

    private final String rootPath;
    private final int bbsPort;
    private InetSocketAddress self;
    private String selfPublicString;

    // 以下、同期する。
    private String jceError;
    private String serverError;
    private String closePortWarning;
    private String newProtocolWarning;

    /**
     * 作成する。
     * @param rootPath 作業場の場所
     * @param bbsPort 2ch サーバのポート番号
     */
    public TrayGui(final String rootPath, final int bbsPort) {
        if (SystemTray.isSupported()) {
            this.tray = SystemTray.getSystemTray();
        } else {
            this.tray = null;
        }
        this.taskQueue = new LinkedBlockingQueue<>();

        this.icon = null;
        this.suicideDialog = null;

        this.rootPath = rootPath;
        this.bbsPort = bbsPort;

        this.self = null;
        this.selfPublicString = null;

        this.jceError = null;
        this.serverError = null;
        this.closePortWarning = null;
        this.newProtocolWarning = null;
    }

    private synchronized InetSocketAddress getSelf() {
        return this.self;
    }

    private synchronized String getSelfPublicString() {
        return this.selfPublicString;
    }

    private synchronized String getJceError() {
        return this.jceError;
    }

    private synchronized String getServerError() {
        return this.serverError;
    }

    private synchronized String getClosePortWarning() {
        return this.closePortWarning;
    }

    private synchronized String getNewProtocolWarning() {
        return this.newProtocolWarning;
    }

    private synchronized void setJceError(final String jceError) {
        this.jceError = jceError;
    }

    private synchronized void setServerError(final String serverError) {
        this.serverError = serverError;
    }

    private synchronized void setClosePortWarning(final String closePortWarning) {
        this.closePortWarning = closePortWarning;
    }

    private synchronized void setNewProtocolWarning(final String newProtocolWarning) {
        this.newProtocolWarning = newProtocolWarning;
    }

    @Override
    public GuiCommand take() throws InterruptedException {
        return this.taskQueue.take();
    }

    private synchronized String getPeerInfoString() {
        final StringBuilder buff = new StringBuilder();
        final InetSocketAddress curSelf = getSelf();
        if (this.newProtocolWarning != null) {
            buff.append(this.newProtocolWarning).append(", ").append(System.lineSeparator());
        }
        if (this.jceError != null) {
            buff.append(this.jceError).append(", ").append(System.lineSeparator());
        }
        if (this.serverError != null) {
            buff.append(this.serverError).append(", ").append(System.lineSeparator());
        } else if (curSelf == null) {
            if (this.closePortWarning != null) {
                buff.append(this.closePortWarning).append(", ").append(System.lineSeparator());
            } else {
                buff.append("ぼっち, ").append(System.lineSeparator());
            }
        } else {
            buff.append(curSelf).append(", ").append(System.lineSeparator());
        }
        return buff.append("2chポート:").append(Integer.toString(this.bbsPort)).append(", ").append(System.lineSeparator())
                .append("作業場:").append(this.rootPath).append(", ").append(System.lineSeparator())
                .toString();
    }

    @Override
    public void start(final ExecutorService executor) {
        if (this.tray == null) {
            LOG.log(Level.WARNING, "システムトレイが無いようです。");
            return;
        }

        final PopupMenu menu = new PopupMenu();

        /*
         * 俺の環境だとなぜかアイコンサイズが 24x24 になるし、背景も透けないけど、
         * Windows だと大丈夫っぽいので気にしない。
         */
        this.icon = new TrayIcon(IconImages.getLogo(), "ちらしの裏", menu);

        /*
         * 個体情報の表示。
         * クリックしたら、個体情報を表示する。
         */
        this.icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                TrayGui.this.icon.displayMessage("個体情報", getPeerInfoString(), TrayIcon.MessageType.NONE);
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
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection("^" + getSelfPublicString()), null);
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

        // // -------------------------------------------------
        // executor.submit(new Callable<Void>() {
        // @Override
        // public Void call() throws Exception {
        // Thread.sleep(15 * 1_000L);
        // TrayGui.this.icon.setPopupMenu(null);
        // menu.removeAll();
        // ConcurrentFunctions.completePut(new ShutdownCommand(), TrayGui.this.taskQueue);
        // return null;
        // }
        // });
        // // -------------------------------------------------
    }

    @Override
    public void close() {
        if (this.tray == null) {
            return;
        }
        if (this.icon != null) {
            this.tray.remove(this.icon);
            LOG.log(Level.FINEST, "トレイアイコンを削除しました。");
        }
        if (this.suicideDialog != null) {
            this.suicideDialog.dispose();
            LOG.log(Level.FINEST, "終了ダイアログを破棄しました。");
        }
    }

    final AtomicReference<String> warnings = new AtomicReference<>("");

    private void printWarnings() {
        if (this.tray == null) {
            return;
        }
        while (true) {
            final StringBuilder buff = new StringBuilder();
            for (final String warning : new String[] { getJceError(), getServerError(), (getSelf() == null ? getClosePortWarning() : null),
                    getNewProtocolWarning() }) {
                if (warning != null) {
                    buff.append(warning).append(", ").append(System.lineSeparator());
                }
            }
            final String newWarnings = buff.toString();
            final String oldWarnings = this.warnings.get();
            if (newWarnings.equals(oldWarnings)) {
                break;
            } else {
                if (this.warnings.compareAndSet(oldWarnings, newWarnings)) {
                    if (newWarnings.length() > 0) {
                        this.icon.displayMessage("警告", buff.toString(), TrayIcon.MessageType.WARNING);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void setSelf(final InetSocketAddress self, final String publicString) {
        synchronized (this) {
            this.self = self;
            this.selfPublicString = publicString;
        }
        printWarnings();
    }

    @Override
    public void displayJceError() {
        setJceError("JCEの制限が解除されていないようです");
        printWarnings();
    }

    @Override
    public void displayServerError() {
        setServerError("サーバーを起動できません");
        printWarnings();
    }

    @Override
    public void displayClosePortWarning(final int port) {
        setClosePortWarning("ポート" + Integer.toString(port) + "が開いていないかもしれません");
        printWarnings();
    }

    @Override
    public void displayNewProtocolWarning(final long major, final long minor) {
        final String msg;
        if (major > 0) {
            if (minor > 0) {
                msg = "この個体より " + Long.toString(major) + " 段階と " + Long.toString(minor) + " 歩だけ新しい個体がいるようです";
            } else {
                msg = "この個体より " + Long.toString(major) + " 段階だけ新しい個体がいるようです";
            }
        } else {
            msg = "この個体より " + Long.toString(minor) + " 歩だけ新しい個体がいるようです";
        }
        setNewProtocolWarning(msg);
        printWarnings();
    }

}
