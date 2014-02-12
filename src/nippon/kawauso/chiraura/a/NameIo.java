/**
 * 
 */
package nippon.kawauso.chiraura.a;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.container.Pair;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.logging.LoggingFunctions;

/**
 * @author chirauraNoSakusha
 */
final class NameIo {

    // インスタンス化防止。
    private NameIo() {}

    private static final Logger LOG = Logger.getLogger(NameIo.class.getName());

    private static Pair<String, String> fromText(final String line) throws MyRuleException {
        final String str = line.trim();
        if (str.isEmpty() || str.startsWith("#")) {
            // 空行、コメント行は無視。
            return null;
        }

        final int pos = str.indexOf(' ');
        if (pos < 0) {
            throw new MyRuleException("No space.");
        }

        final String board = str.substring(0, pos).trim();
        final String name = str.substring(pos + 1).trim();
        if (board.isEmpty() || name.isEmpty()) {
            throw new MyRuleException("Empty item.");
        } else {
            return new Pair<>(board, name);
        }
    }

    static Map<String, String> fromTextFile(final File input) {
        final Map<String, String> result = new HashMap<>();

        if (!input.exists()) {
            return result;
        }

        try (BufferedReader buff = new BufferedReader(new InputStreamReader(new FileInputStream(input), Global.INTERNAL_CHARSET))) {
            for (String line; (line = buff.readLine()) != null;) {
                try {
                    final Pair<String, String> entry = fromText(line);
                    if (entry != null) {
                        result.put(entry.getFirst(), entry.getSecond());
                    }
                } catch (final RuntimeException | MyRuleException e) {
                    LOG.log(Level.WARNING, input.getPath() + " の \"" + line + "\" の読み取りに失敗しました", e);
                }
            }
        } catch (final IOException e) {
            final File backup = new File(input.getParent(), input.getName() + "." + LoggingFunctions.getShortDate(System.currentTimeMillis()) + ".error");
            if (!input.renameTo(backup)) {
                LOG.log(Level.WARNING, "壊れた " + input.getPath() + " を " + backup.getPath() + " として保存しました", e);
            } else {
                LOG.log(Level.WARNING, "壊れた " + input.getPath() + " を " + backup.getPath() + " として保存できませんでした", e);
            }
        }
        return result;
    }
}
