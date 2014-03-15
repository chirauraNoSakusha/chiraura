/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * 基本 1 行で SimpleFormatter にスレッド番号を加えた程度の情報を表示する。
 *
 * <pre>yyyy/MM/dd HH:mm:ss LEVEL CLASS METHOD (THREAD): MESSAGE[:\nTHROWABLE]\n</pre>
 * @author chirauraNoSakusha
 */
public final class OneLineThreadFormatter extends Formatter {

    /*
     * 下の行を有効にするとデッドロックする。
     * たぶん、Formatterをこのクラスにしようとしたときに
     * このクラスが初めて読み込まれて止まる。
     */
    // private static final Logger LOG = Logger.getLogger(OneLineThreadFormatter.class.getName());

    @Override
    public String format(final LogRecord record) {
        final StringBuilder sb = (new StringBuilder(LoggingFunctions.getSimpleDate(record.getMillis())))
                .append(' ').append(record.getLevel().getName())
                .append(' ').append(record.getSourceClassName())
                .append(' ').append(record.getSourceMethodName())
                .append(" (").append(record.getThreadID())
                .append("): ").append(formatMessage(record));
        final Throwable throwable = record.getThrown();
        if (throwable != null) {
            sb.append(":").append(System.lineSeparator()).append(LoggingFunctions.getStackTraceString(throwable));
        } else {
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }

}
