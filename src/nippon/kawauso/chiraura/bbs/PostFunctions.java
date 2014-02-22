/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.converter.Base64;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;

/**
 * @author chirauraNoSakusha
 */
final class PostFunctions {

    // インスタンス化防止。
    private PostFunctions() {}

    private static final Logger LOG = Logger.getLogger(PostFunctions.class.getName());

    private final static long[] HASH_CODE_CACHE;
    static {
        final String[] keys = new String[] {
                "java.version",
                "java.vendor",
                "java.vendor.url",
                "java.home",
                "java.vm.specification.version",
                "java.vm.specification.vendor",
                "java.vm.specification.name",
                "java.vm.version",
                "java.vm.vendor",
                "java.vm.name",
                "java.specification.version",
                "java.specification.vendor",
                "java.specification.name",
                "java.class.version",
                "java.class.path",
                "java.library.path",
                "java.io.tmpdir",
                "java.compiler",
                "java.ext.dirs",
                "os.name",
                "os.arch",
                "os.version",
                "file.separator",
                "path.separator",
                "line.separator",
                "user.name",
                "user.home",
                "user.dir",
        };
        final List<Integer> hashCodes = new ArrayList<>();
        for (final String key : keys) {
            final String value = System.getProperty(key);
            if (value != null) {
                hashCodes.add(value.hashCode());
            } else {
                LOG.log(Level.FINEST, "環境変数 {0} は定義されていません。", key);
            }
        }
        HASH_CODE_CACHE = new long[hashCodes.size()];
        for (int i = 0; i < HASH_CODE_CACHE.length; i++) {
            HASH_CODE_CACHE[i] = hashCodes.get(i);
        }
    }

    /**
     * 書き込み ID を計算する。
     * @param boardName 板名
     * @param date 日時
     * @return 書き込み ID
     */
    static long calculateId(final String boardName, final long date, final InetAddress source) {
        final long prime = 31;
        long authorId = 0;
        // 書き込み元。
        if (!source.isLoopbackAddress() && !source.isLinkLocalAddress() && !source.isSiteLocalAddress()) {
            // グローバル IP だけ有効。自作自演対策。
            authorId = prime * authorId + source.hashCode();
        }
        // 板名。
        authorId = prime * authorId + boardName.hashCode();
        // 日時。
        authorId = prime * authorId + date / Duration.DAY;
        // 環境。
        for (final long hashCode : HASH_CODE_CACHE) {
            authorId = prime * authorId + hashCode;
        }
        return authorId;
    }

    /**
     * 書き込み ID を文字列にする。
     * @param authorId 書き込み ID
     * @return 書き込み ID の文字列
     */
    static String idToString(final long authorId) {
        final int length = 6;
        final byte[] buff = BytesConversion.toBytes("cl", authorId);
        if (buff.length > length) {
            return Base64.toBase64(Arrays.copyOfRange(buff, buff.length - length, buff.length));
        } else {
            return Base64.toBase64(Arrays.copyOf(buff, length));
        }
    }

    private static final String[] EMPTY_AUTHORS = new String[] {
            "よみびとしらず",
            "名無し",
            "名無しの権兵衛",
            "へのへのもへじ",
    };

    private static final Pattern GREATER_SYMBOL = Pattern.compile(">");
    private static final Pattern LESS_SYMBOL = Pattern.compile("<");

    private static String replaceGtAndLt(final String input) {
        return LESS_SYMBOL.matcher(GREATER_SYMBOL.matcher(input).replaceAll("&gt;")).replaceAll("&lt;");
    }

    static String wrapTitle(final String title) {
        return replaceGtAndLt(title);
    }

    static final int ID_LENGTH = 12;
    private static final Pattern EMPTY_PATTERN = Pattern.compile("^\\s*$");
    private static final Pattern KOTEHAN_SYMBOL = Pattern.compile("◆");

    /**
     * 書き込んだ人の名前を整形する。
     * @param author 元の名前
     * @param boardName 板名
     * @param menu 板固有の名無し
     * @return 整形した名前
     */
    static String wrapAuthor(final String author, final String boardName, final Menu menu) {
        // 名無し。
        if (EMPTY_PATTERN.matcher(author).find()) {
            final String name = menu.getNanashi(boardName);
            if (name != null) {
                return name;
            } else {
                return EMPTY_AUTHORS[Math.abs(boardName.hashCode() % EMPTY_AUTHORS.length)];
            }
        }

        // コテハン。
        final int index = author.indexOf('#');
        if (index < 0) {
            return replaceGtAndLt(KOTEHAN_SYMBOL.matcher(author).replaceAll("◇"));
        }
        final String hash = HashValue.calculateFromBytes(author.substring(index).getBytes(Global.INTERNAL_CHARSET)).toBase64();
        return (new StringBuilder(replaceGtAndLt(KOTEHAN_SYMBOL.matcher(author.substring(0, index)).replaceAll("◇"))))
                .append('◆').append(hash.substring(Math.max(0, hash.length() - ID_LENGTH))).toString();

    }

    static String wrapMail(final String mail) {
        return replaceGtAndLt(mail);
    }

    private static final Pattern SEPARATOR_SYMBOL = Pattern.compile("(\r\n|\r|\n)");

    static String wrapMessage(final String message) {
        return SEPARATOR_SYMBOL.matcher(replaceGtAndLt(message)).replaceAll(" <BR> ");
    }

    /*
     * 以下、動作試験。
     */

    private static void sample1() throws UnknownHostException {
        for (final InetAddress source : new InetAddress[] { InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 }),
                InetAddress.getByAddress(new byte[] { 1, 2, 3, 5 }) }) {
            for (final String board : new String[] { "test", "death" }) {
                for (final long date : new long[] { System.currentTimeMillis(), System.currentTimeMillis() + 12 * Duration.HOUR,
                        System.currentTimeMillis() + Duration.DAY }) {
                    System.out.printf("%s, %5s %s %s\n", source.toString(), board, LoggingFunctions.getSimpleDate(date),
                            idToString(calculateId(board, date, source)));
                }
            }
        }
    }

    private static void sample2() throws UnknownHostException {
        final InetAddress source = InetAddress.getByAddress(new byte[] { 1, 2, 3, 4 });
        System.out.println("TYPE_ID: " + idToString(calculateId("test", System.currentTimeMillis(), source)));
    }

    public static void main(final String[] args) throws UnknownHostException {
        sample2();
        System.out.println();
        sample1();

        // System.out.println(isEmptyAuthor(""));
        // System.out.println(isEmptyAuthor(" "));
        // System.out.println(isEmptyAuthor(" a "));
        // System.out.println(isEmptyAuthor("a"));

        // System.out.println(isValidAuthor("aaaa"));
        // System.out.println(isValidAuthor("aa<aa"));
        // System.out.println(isValidAuthor("aa\01aa"));
        // System.out.println("aa\00aa");

        // System.out.println(wrapAuthor("名無し", "aho"));
        // System.out.println(wrapAuthor("名無し◆aaaa", "aho"));
        // System.out.println(wrapAuthor("名無し#aaaa", "aho"));

        System.out.println(wrapMessage("あああ\nいい\r\nううう\r\n\n\r\r\n\n\r"));
        System.out.println("Aho " + Integer.toBinaryString(0 + '<') + " " + Integer.toBinaryString(0 + '>') + " " + Integer.toBinaryString(0 + '◆') + " "
                + Integer.toBinaryString(0 | '<' | '>' | '◆'));
    }

}
