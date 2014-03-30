package nippon.kawauso.chiraura.lib.connection;

import java.net.InetSocketAddress;

/**
 * @author chirauraNoSakusha
 */
public final class InetAddressFunctions {

    // インスタンス化防止。
    private InetAddressFunctions() {}

    /**
     * 自称する IP アドレスとしてより相応しい方を選ぶ。
     * @param oldOne 古い方
     * @param newOne 新しい方
     * @return 相応しい方
     */
    public static InetSocketAddress selectBetter(final InetSocketAddress oldOne, final InetSocketAddress newOne) {
        if (oldOne == null) {
            return newOne;
        } else if (newOne == null) {
            return oldOne;
        } else if (oldOne.getAddress().isLoopbackAddress()) {
            if (newOne.getAddress().isLoopbackAddress()) {
                // 同程度の場合は馴染みのある方を選ぶ。
                return oldOne;
            } else {
                return newOne;
            }
        } else if (newOne.getAddress().isLoopbackAddress()) {
            return oldOne;
        } else if (oldOne.getAddress().isLinkLocalAddress()) {
            if (newOne.getAddress().isLinkLocalAddress()) {
                // 同程度の場合は馴染みのある方を選ぶ。
                return oldOne;
            } else {
                return newOne;
            }
        } else if (newOne.getAddress().isLinkLocalAddress()) {
            return oldOne;
        } else if (oldOne.getAddress().isSiteLocalAddress()) {
            if (newOne.getAddress().isSiteLocalAddress()) {
                // 同程度の場合は馴染みのある方を選ぶ。
                return oldOne;
            } else {
                return newOne;
            }
        } else if (newOne.getAddress().isSiteLocalAddress()) {
            return oldOne;
        } else {
            return newOne;
        }
    }

}
