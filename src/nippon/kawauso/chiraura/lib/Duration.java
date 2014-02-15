/**
 * 
 */
package nippon.kawauso.chiraura.lib;

import java.util.Arrays;
import java.util.List;

import nippon.kawauso.chiraura.lib.container.Pair;

/**
 * 期間。
 * @author chirauraNoSakusha
 */
public final class Duration {

    // インスタンス化防止。
    private Duration() {}

    /**
     * 秒。
     */
    public static final long SECOND = 1_000L;

    /**
     * 分。
     */
    public static final long MINUTE = 60 * SECOND;

    /**
     * 時間。
     */
    public static final long HOUR = 60 * MINUTE;

    /**
     * 日。
     */
    public static final long DAY = 24 * HOUR;

    /**
     * 月。
     */
    public static final long MONTH = 30 * DAY;

    /**
     * 年。
     */
    public static final long YEAR = 365 * DAY;

    /**
     * 文字列にする。
     * @param milliSeconds 期間 (ミリ秒)
     * @return 期間を表す文字列
     */
    public static String toString(final long milliSeconds) {

        final StringBuilder buff = new StringBuilder();

        final List<Pair<Long, String>> list = Arrays.asList(
                new Pair<>(0L, (String) null),
                new Pair<>(1L, "ミリ秒"),
                new Pair<>(SECOND, "秒"),
                new Pair<>(MINUTE, "分"),
                new Pair<>(HOUR, "時間"),
                new Pair<>(DAY, "日"),
                new Pair<>(MONTH, "ヶ月"),
                new Pair<>(YEAR, "年"),
                new Pair<>(Long.MAX_VALUE, (String) null)
                );

        for (int i = 1; i < list.size() - 1; i++) {
            if (milliSeconds < list.get(i + 1).getFirst()) {
                final long unit = list.get(i).getFirst();
                final long val = Math.round(milliSeconds / (double) unit);
                buff.append(Long.toString(val)).append(list.get(i).getSecond());
                if (Math.abs(val * unit - milliSeconds) >= list.get(i - 1).getFirst()) {
                    buff.append("くらい");
                }
                break;
            }
        }

        return buff.toString();
    }

}
