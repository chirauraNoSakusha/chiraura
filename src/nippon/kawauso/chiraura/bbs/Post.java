/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.net.ProtocolException;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.StringFunctions;
import nippon.kawauso.chiraura.lib.container.Pair;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 書き込みとかスレッド作成関係。
 * @author chirauraNoSakusha
 */
final class Post {

    private static final Logger LOG = Logger.getLogger(Post.class.getName());

    private static final Charset CHARSET = Charset.forName("Shift_JIS");

    static final String SUBMIT_LABEL_1 = "新規スレッド作成";
    static final String SUBMIT_LABEL_2 = "書き込む";

    static enum Entry {
        BBS,
        SUBJECT,
        KEY,
        TIME,
        FROM,
        MAIL,
        MESSAGE,
        SUBMIT,

        ;

        private static Pair<Entry, String> decode(final String token) throws ProtocolException {
            final int index = token.indexOf('=');
            if (index < 0) {
                throw new ProtocolException("Unknown format: \"" + token + "\".");
            }
            final String name = token.substring(0, index).toUpperCase();
            final Entry entry;
            try {
                entry = Entry.valueOf(name);
            } catch (final IllegalArgumentException e) {
                return null;
            }

            final String str = token.substring(index + 1);
            String value;
            try {
                value = StringFunctions.urlDecode(str, CHARSET);
            } catch (final MyRuleException e) {
                if (entry == SUBMIT && (str.equals(SUBMIT_LABEL_1) || str.equals(SUBMIT_LABEL_2))) { // なぜかエンコードしないで送ってくるブラウザがある。
                    value = str;
                } else {
                    throw (ProtocolException) (new ProtocolException()).initCause(e);
                }
            }

            return new Pair<>(entry, value);
        }

        static Map<Entry, String> decodeEntries(final String line) throws ProtocolException {
            final String str;
            if (line.endsWith(Http.SEPARATOR)) {
                // ギコナビが、なぜかコンテンツの末尾にCRLFを含めてくるので、応急処置。
                str = line.substring(0, line.length() - Http.SEPARATOR.length());
            } else {
                str = line;
            }
            final Map<Entry, String> entries = new EnumMap<>(Entry.class);
            for (final String token : str.split("&")) {
                final Pair<Entry, String> entry = decode(token);
                if (entry != null) {
                    entries.put(entry.getFirst(), entry.getSecond());
                } else {
                    LOG.log(Level.WARNING, "不明な項目 ( \"{0}\" )を飛ばします。", line);
                }
            }
            return entries;
        }
    }

    static enum Result {
        TRUE,
        FALSE,
        ERROR,
        CHECK,
        COOKIE,

        ;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }

    }

}
