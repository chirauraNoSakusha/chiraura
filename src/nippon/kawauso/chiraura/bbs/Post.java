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

            final String value;
            try {
                value = StringFunctions.urlDecode(token.substring(index + 1), CHARSET);
            } catch (final MyRuleException e) {
                throw (ProtocolException) (new ProtocolException()).initCause(e);
            }

            return new Pair<>(entry, value);
        }

        static Map<Entry, String> decodeEntries(final String line) throws ProtocolException {
            final Map<Entry, String> entries = new EnumMap<>(Entry.class);
            for (final String token : line.split("&")) {
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

}
