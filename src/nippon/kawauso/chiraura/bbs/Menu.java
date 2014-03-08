/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.http.Http;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;

/**
 * bbsmenu.html 相当にデフォルト名無しの情報を追加したもの。
 * 最初に読み込んで、後は不変なので、同期しない。
 * @author chirauraNoSakusha
 */
public final class Menu implements Content {

    private static final Logger LOG = Logger.getLogger(Menu.class.getName());

    static final class Entry {
        private final String name; // test とか。
        private final String label; // てすと とか。
        private final String nanashi;

        private Entry(final String name, final String label, final String nanashi) {
            this.name = name;
            this.label = label;
            this.nanashi = nanashi;
        }
    }

    static final class Category {
        private final String label;
        private final List<Entry> entries;

        private Category(final String label, final List<Entry> entries) {
            this.label = label;
            this.entries = entries;
        }
    }

    private final long date;
    private final List<Category> categories;
    private final Map<String, Entry> boardToEntry;

    private Menu(final long date, final List<Category> categories, final Map<String, Entry> boardToEntry) {
        this.date = date;
        this.categories = categories;
        this.boardToEntry = boardToEntry;
    }

    Menu() {
        this(System.currentTimeMillis(), new ArrayList<Category>(), new HashMap<String, Entry>());
    }

    @Override
    public long getUpdateDate() {
        return this.date;
    }

    @Override
    public long getNetworkTag() {
        return this.date;
    }

    /**
     * @param host クライアントが送ってきた Host フィールド。非 null
     * @param port 待受ポート
     * @return メニュー
     */
    String toNetworkString(final String host, final int port) {
        final String after;
        if (host.indexOf(':') < 0 && port != Http.DEFAULT_PORT) {
            after = (new StringBuilder(host)).append(':').append(port).toString();
        } else {
            after = host;
        }

        final String separator = Http.SEPARATOR; // TODO 何が正解なのか？

        final StringBuilder buff = new StringBuilder()
                .append("<HTML>").append(separator)
                .append("<HEAD>").append(separator)
                .append("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=Shift_JIS\">").append(separator)
                .append("<TITLE>BBS MENU for chiraura</TITLE>").append(separator)
                .append("</HEAD>").append(separator)
                .append("<BODY>").append(separator);
        for (final Category category : this.categories) {
            buff.append("<br><br><b>").append(category.label).append("</b><br>").append(separator);
            for (final Entry entry : category.entries) {
                buff.append("<a href=\"http://").append(after).append('/').append(entry.name).append("/\">").append(entry.label).append("</a><br>")
                        .append(separator);
            }
        }
        return buff
                .append("</BODY>").append(separator)
                .append("</HTML>")
                .toString();
    }

    @Override
    public String getContentType() {
        return Http.ContentType.TEXT_HTML.toString();
    }

    String getNanashi(final String board) {
        final Entry entry = this.boardToEntry.get(board);
        if (entry == null) {
            return null;
        } else {
            return entry.nanashi;
        }
    }

    String getLabel(final String board) {
        final Entry entry = this.boardToEntry.get(board);
        if (entry == null) {
            return null;
        } else {
            return entry.label;
        }
    }

    /**
     * 読み込む。
     * @param input 読み込むファイル
     * @return 読み込んだメニュー
     */
    public static Menu fromTextFile(final File input) {

        final Map<String, Entry> boardToEntry = new HashMap<>();
        final List<Category> categories = new ArrayList<>();

        if (!input.exists()) {
            return new Menu(System.currentTimeMillis(), categories, boardToEntry);
        }

        final long date = input.lastModified();

        final Pattern spaces = Pattern.compile("\\s+");

        try (BufferedReader buff = new BufferedReader(new InputStreamReader(new FileInputStream(input), Global.INTERNAL_CHARSET))) {
            String categoryLabel = "カテゴリ無し";
            List<Entry> entries = new ArrayList<>();
            for (String line; (line = buff.readLine()) != null;) {
                final String[] tokens = spaces.split(line.trim()); // split の結果の先頭が長さ 0 の文字列にならないように。

                if (tokens.length == 0 || tokens[0].startsWith("#")) {
                    continue;
                } else if (tokens.length == 1) {
                    // カテゴリ。
                    if (!entries.isEmpty()) {
                        categories.add(new Category(categoryLabel, entries));
                        entries = new ArrayList<>();
                    }
                    categoryLabel = tokens[0];
                } else if (tokens.length == 2) {
                    // 板名 板ラベル。
                    final Entry entry = new Entry(tokens[0], tokens[1], null);
                    boardToEntry.put(tokens[0], entry);
                    entries.add(entry);
                } else {
                    if (tokens.length > 3) {
                        LOG.log(Level.WARNING, "{0} の {1} のような 4 つ以上の単語には対応していません。", new Object[] { input.getPath(), line });
                    }
                    // 板名 板ラベル デフォルト名無し
                    final Entry entry = new Entry(tokens[0], tokens[1], tokens[2]);
                    boardToEntry.put(tokens[0], entry);
                    entries.add(entry);
                }
            }

            if (!entries.isEmpty()) {
                categories.add(new Category(categoryLabel, entries));
                entries = new ArrayList<>();
            }
        } catch (final IOException e) {
            final File backup = new File(input.getParent(), input.getName() + "." + LoggingFunctions.getShortDate(System.currentTimeMillis()) + ".error");
            LOG.log(Level.WARNING, "異常が発生しました", e);
            if (!input.renameTo(backup)) {
                LOG.log(Level.INFO, "壊れた {0} を {1} として保存しました。", new Object[] { input.getPath(), backup.getPath() });
            } else {
                LOG.log(Level.INFO, "壊れた {0} を {1} として保存することもできませんでした。", new Object[] { input.getPath(), backup.getPath() });
            }
        }

        return new Menu(date, categories, boardToEntry);
    }
}
