package nippon.kawauso.chiraura.util;

import java.net.InetSocketAddress;

import nippon.kawauso.chiraura.lib.Mosaic;

/**
 * @author chirauraNoSakusha
 */
final class PeerMosaicer {

    /**
     * 個体情報を公開可能な形で表示する。
     * @param args 長さ 2。
     *            1 つ目はホスト。
     *            2 つ目はポート
     */
    public static void main(final String[] args) {
        final InetSocketAddress peer = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        System.out.println(Mosaic.peerTo(peer));
    }

}
