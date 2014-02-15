/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.a;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.StringFunctions;
import nippon.kawauso.chiraura.lib.container.Pair;

/**
 * 引き数。
 * @author chirauraNoSakusha
 */
final class Option {

    private static final Logger LOG = Logger.getLogger(Option.class.getName());

    private static final String APPLICATION_LABEL = "chiraura";

    /**
     * 項目。
     * @author chirauraNoSakusha
     */
    static enum Item {
        help(null, "この文章を表示"),
        config(null, "設定ファイル"),

        root(System.getProperties().get("user.dir").toString(), "使用ディレクトリ"),
        port(Integer.toString(24_804), "待機ポート番号"),
        shutdownTimeout(Long.toString(60 * 1_000L) /* 1 分 */, "終処理の完了を待つ時間 (ミリ秒)"),
        consoleLogLevel(Level.INFO.getName(), "画面に出力するログの程度"),
        fileLogLevel(Global.isDebug() ? Level.ALL.getName() : Level.INFO.getName(), "ファイルに出力するログの程度"),
        chunkCacheCapacity(Integer.toString(300), "データ片をキャッシュする数"),
        indexCacheCapacity(Integer.toString(10_000), "データ片の概要をキャッシュする数"),
        rangeCacheCapacity(Integer.toString(40), "データ片の概要の範囲取得結果をキャッシュする数"),
        peerCapacity(Integer.toString(1_000), "個体を保持する数"),
        receiveBufferSize(Integer.toString(128 * 1024) /* 128 KB */, "受信バッファサイズ (バイト)"),
        sendBufferSize(Integer.toString(64 * 1024) /* 64 KB */, "送信バッファサイズ (バイト)"),
        maintenanceInterval(Long.toString(60 * 1_000L) /* 1 分 */, "保守間隔 (ミリ秒)"),
        sleepTime(Long.toString(30 * 60 * 1_000L) /* 30 分 */, "何もすることが無い場合にふて寝する時間 (ミリ秒)"),
        backupInterval(Long.toString(5 * 60 * 1_000L) /* 5 分 */, "データの保守間隔 (ミリ秒)"),
        connectionTimeout(Long.toString(15 * 60 * 1_000L) /* 15 分 */, "非通信接続を切断するまでの時間 (ミリ秒)"),
        operationTimeout(Long.toString(60 * 1_000L) /* 1 分 */, "通信を要する操作を諦めるまでの時間 (ミリ秒)"),
        cacheLogCapacity(Integer.toString(10_000), "データ通信の結果をキャッシュする数"),
        cacheDuration(Long.toString(30 * 1_000L) /* 30 秒 */, "データ通信の結果を使い回す期間"),
        idLifetime(Long.toString(7 * 24 * 60 * 60 * 1_000L) /* 1 週間 */, "個体識別用公開鍵を使い回す期間 (ミリ秒)"),
        publicKeyLifetime(Long.toString(24 * 60 * 60 * 1_000L) /* 1 日 */, "通信用公開鍵を使い回す期間 (ミリ秒)"),
        commonKeyLifetime(Long.toString(60 * 60 * 1_000L) /* 1 時間 */, "通信用共通鍵を使い回す期間 (ミリ秒)"),
        blacklistCapacity(Integer.toString(200), "拒否対象の個体を保持する数"),
        blacklistTimeout(Long.toString(30 * 60 * 1_000L) /* 30 分 */, "個体の拒否を解除するまでの時間 (ミリ秒)"),
        potCapacity(Integer.toString(1_000), "予備として個体を保持する数"),
        addressCacheCapacity(Integer.toString(1_000), "個体の論理位置の計算結果をキャッシュする数"),
        activeAddressLogCapacity(Integer.toString(1_000), "直接通信して得た個体の論理位置をキャッシュする数"),
        activeAddressDuration(Long.toString(5 * 60 * 1_000L) /* 5 分 */, "直接通信して得た個体の論理位置を伝聞より優先させる期間"),
        bbsPort(Integer.toString(22_266), "BBS の待機ポート番号"),
        bbsConnectionTimeout(Long.toString(10 * 60 * 1_000L) /* 10 分 */, "BBS の非通信接続を切断するまでの時間 (ミリ秒)"),
        bbsInternalTimeout(Long.toString(3 * 60 * 1_000L) /* 3 分 */, "BBS の応答を諦めるまでの時間 (ミリ秒)"),
        bbsUpdateThreshold(Long.toString(10 * 60 * 1_000L) /* 10 分 */, "BBS の更新を自粛する期間 (ミリ秒)"),
        gui(Boolean.toString(!Global.isDebug()), "GUI を使用するや否や"),
        guiBootDuration(Long.toString(3 * 60 * 1_000L) /* 3 分 */, "起動中とみなす時間 (ミリ秒)"),
        guiMaxDelay(Long.toString(60 * 60 * 1_000L) /* 1 時間 */, "更新報告の最大遅延時間 (ミリ秒)"),
        guiInterval(Long.toString(60 * 60 * 1_000L) /* 1 時間 */, "警告表示の間隔 (ミリ秒)"),

        ;

        private final String initial;
        private final String descryption;

        private Item(final String initial, final String descryption) {
            this.initial = initial;
            this.descryption = descryption;
        }

        String getInitialValue() {
            return this.initial;
        }

    }

    /*
     * 設定は以下の順に反映される。
     * 1. 個人設定: {ユーザーディレクトリ}/.chiraura
     * 2. 作業場設定: {カレントディレクトリ}/chiraura.ini
     * 3. 指定の設定: {指定された設定ファイル}
     * 4. コマンドライン引数
     */

    private final Queue<Pair<Level, String>> logs;
    private final EnumMap<Item, String> items;

    Option(final String... args) throws FileNotFoundException, IOException {
        this.logs = new LinkedList<>();
        this.items = loadOptions(args);
    }

    String get(final Item item) {
        return this.items.get(item);
    }

    void afterStartLogging() {
        while (!this.logs.isEmpty()) {
            final Pair<Level, String> log = this.logs.poll();
            LOG.log(log.getFirst(), log.getSecond());
        }
    }

    private EnumMap<Item, String> loadOptions(final String... args) throws FileNotFoundException, IOException {
        // 読み込み。
        final EnumMap<Item, String> argumentItems = loadArguments(args);
        if (argumentItems.containsKey(Item.help)) {
            // ヘルプの表示ならさっさと返る。
            return argumentItems;
        }

        final String config = argumentItems.remove(Item.config);
        final EnumMap<Item, String> givenConfigItems = loadGivenConfig(config);
        final EnumMap<Item, String> workspaceConfigItems = loadWorkspaceConfig();
        final EnumMap<Item, String> userConfigItems = loadUserConfig();

        // 統合。
        final EnumMap<Item, String> unifiedItems = new EnumMap<>(Item.class);
        if (!isSubset(unifiedItems, userConfigItems)) {
            if (unifiedItems.isEmpty()) {
                this.logs.add(new Pair<>(Level.INFO, "個人設定 ( " + userConfig().getPath() + " ) を読み込みました。"));
            } else {
                this.logs.add(new Pair<>(Level.INFO, "個人設定 ( " + userConfig().getPath() + " ) で上書きしました。"));
            }
            unifiedItems.putAll(userConfigItems);
        }
        if (!isSubset(unifiedItems, workspaceConfigItems)) {
            if (unifiedItems.isEmpty()) {
                this.logs.add(new Pair<>(Level.INFO, "作業場設定 ( " + workspaceConfig().getPath() + " ) を読み込みました。"));
            } else {
                this.logs.add(new Pair<>(Level.INFO, "作業場設定 ( " + workspaceConfig().getPath() + " ) で上書きしました。"));
            }
            unifiedItems.putAll(workspaceConfigItems);
        }
        if (!isSubset(unifiedItems, givenConfigItems)) {
            if (unifiedItems.isEmpty()) {
                this.logs.add(new Pair<>(Level.INFO, "指定の設定 ( " + config + " ) を読み込みました。"));
            } else {
                this.logs.add(new Pair<>(Level.INFO, "指定の設定 ( " + config + " ) で上書きしました。"));
            }
            unifiedItems.putAll(givenConfigItems);
        }
        if (!isSubset(unifiedItems, argumentItems)) {
            if (unifiedItems.isEmpty()) {
                this.logs
                        .add(new Pair<>(
                                Level.INFO,
                                "引き数を反映させました。"));
            } else {
                this.logs.add(new Pair<>(Level.INFO, "引き数で上書きしました。"));
            }
            unifiedItems.putAll(argumentItems);
        }

        // 初期値で穴埋め。
        for (final Item item : Item.values()) {
            if (!unifiedItems.containsKey(item)) {
                unifiedItems.put(item, item.initial);
            }
        }

        return unifiedItems;
    }

    private static File userConfig() {
        return new File(System.getProperties().get("user.home").toString(), "." + APPLICATION_LABEL);
    }

    private static File workspaceConfig() {
        return new File((String) System.getProperties().get("user.dir"), APPLICATION_LABEL + ".ini");
    }

    /**
     * 含まれるか検査。
     * @param items1 含む方
     * @param items2 含まれる方
     * @return items2 が items1 に含まれる場合のみ true
     */
    private static boolean isSubset(final EnumMap<Item, String> items1, final EnumMap<Item, String> items2) {
        for (final Map.Entry<Item, String> entry : items2.entrySet()) {
            if (!entry.getValue().equals(items1.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static EnumMap<Item, String> loadUserConfig() throws IOException {
        /*
         * 無ければ無視。
         */
        final File config = userConfig();
        if (config.isFile() && config.canRead()) {
            return loadConfig(config);
        } else {
            return new EnumMap<>(Item.class);
        }
    }

    private static EnumMap<Item, String> loadWorkspaceConfig() throws IOException {
        /*
         * 無ければ無視。
         */
        final File config = workspaceConfig();
        if (config.isFile() && config.canRead()) {
            return loadConfig(config);
        } else {
            return new EnumMap<>(Item.class);
        }
    }

    private static EnumMap<Item, String> loadGivenConfig(final String config) throws FileNotFoundException, IOException {
        if (config != null) {
            return loadConfig(new File(config));
        } else {
            return new EnumMap<>(Item.class);
        }
    }

    /**
     * 設定ファイルから読み込む。
     * @param config 設定ファイル
     * @return 設定
     * @throws FileNotFoundException 設定ファイルが無かった場合
     * @throws IOException 読み込み異常
     */
    private static EnumMap<Item, String> loadConfig(final File config) throws FileNotFoundException, IOException {
        final Properties properties = new Properties();
        try (final FileInputStream input = new FileInputStream(config)) {
            properties.load(input);
        }

        final EnumMap<Item, String> items = new EnumMap<>(Item.class);
        for (final Item item : Item.values()) {
            if (properties.contains(item.name())) {
                if (item == Item.help || item == Item.config) {
                    LOG.log(Level.WARNING, "設定ファイルによる \"{0}\" の指定はできません。", item.name());
                    throw new RuntimeException("Invalid option.");
                }
                items.put(item, (String) properties.remove(item.name()));
            }
        }

        for (final String property : properties.stringPropertyNames()) {
            LOG.log(Level.WARNING, "\"{0}\" なる指定は定義されていません。", property);
            throw new RuntimeException("Invalid option.");
        }

        return items;
    }

    /**
     * コマンドライン引数から読み込む。
     * @param args
     * @return
     */
    private static EnumMap<Item, String> loadArguments(final String... args) {
        final EnumMap<Item, String> items = new EnumMap<>(Item.class);
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                // コマンドラインオプション。
                final String name = args[i].substring(1);
                final Item item;
                try {
                    item = Item.valueOf(name);
                } catch (final IllegalArgumentException e) {
                    i++;
                    LOG.log(Level.WARNING, "\"{0}\" なる指定は定義されていません。", name);
                    throw e;
                }
                if (item == Item.help) {
                    // ヘルプ。
                    items.put(Item.help, Boolean.toString(true));
                } else {
                    // その他。
                    i++;
                    if (i < args.length) {
                        items.put(item, args[i]);
                    } else {
                        LOG.log(Level.WARNING, "指定 \"{0}\" に渡す値がありません。", name);
                        throw new RuntimeException("Invalid option.");
                    }
                }
            } else {
                // 設定ファイル名。
                items.put(Item.config, args[i]);
            }
        }
        return items;
    }

    /**
     * オプション デフォルト値 説明
     * という形式で出力。
     * @return
     */
    static String toHelpString() {
        final String nameLabel = "指定子";
        final String initialLabel = "初期値";
        final String descryptionTag = "説明";
        int nameMax = StringFunctions.getWidth(nameLabel);
        int initialMax = StringFunctions.getWidth(initialLabel);
        for (final Item item : Item.values()) {
            final int nameWidth = StringFunctions.getWidth(item.name());
            if (nameMax < nameWidth) {
                nameMax = nameWidth;
            }
            if (item.initial != null) {
                final int initialWidth = StringFunctions.getWidth(item.initial);
                if (initialMax < initialWidth) {
                    initialMax = initialWidth;
                }
            }
        }

        String format;
        format = "%-" + (nameMax - StringFunctions.getWidth(nameLabel) + nameLabel.length()) + "s %-"
                + (initialMax - StringFunctions.getWidth(initialLabel) + initialLabel.length()) + "s %s%n";
        final StringBuilder sb = new StringBuilder(String.format(format, nameLabel, initialLabel, descryptionTag));
        for (final Item item : Item.values()) {
            if (item == Item.help || item.initial == null) {
                format = "%-" + nameMax + "s %-" + initialMax + "s %s%n";
                sb.append(String.format(format, item.name(), "", item.descryption));
            } else if (item.descryption == null) {
                format = "%-" + nameMax + "s %-" + initialMax + "%n";
                sb.append(String.format(format, item.name(), item.initial));
            } else {
                format = "%-" + nameMax + "s %-" + initialMax + "s %s%n";
                sb.append(String.format(format, item.name(), item.initial, item.descryption));
            }
        }
        return sb.toString();
    }

    /**
     * コマンドラインオプションとしてコピペして使える形式で出力。
     * @return 現在の値を列挙した文字列
     */
    String toCommandlineString() {
        int max = 0;
        for (final Item item : Item.values()) {
            if (item == Item.help ||
                    (item.initial == null && this.items.get(item) == null) ||
                    (item.initial != null && item.initial.equals(this.items.get(item)))) {
                // ヘルプ、初期値は表示しない。
                continue;
            }
            if (max < item.name().length()) {
                max = item.name().length();
            }
        }
        final String format = "-%-" + max + "s %s";
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (final Item item : Item.values()) {
            if (item == Item.help ||
                    (item.initial == null && this.items.get(item) == null) ||
                    (item.initial != null && item.initial.equals(this.items.get(item)))) {
                // ヘルプ、初期値は表示しない。
                continue;
            }
            if (first) {
                first = false;
            } else {
                sb.append(String.format(" \\%n"));
            }
            sb.append(String.format(format, item.name(), this.items.get(item)));
        }
        return sb.toString();
    }

    public static void main(final String[] args) throws FileNotFoundException, IOException {
        final Option option = new Option("-root ~/tmp/aho -port 11111".split(" "));
        System.out.println("toStringによる出力");
        System.out.println(option.toCommandlineString());
        System.out.println("totHelpStringによる出力");
        System.out.println(Option.toHelpString());
    }
}
