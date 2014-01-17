/**
 * 
 */
package nippon.kawauso.chiraura.util;

import java.net.InetSocketAddress;

import nippon.kawauso.chiraura.lib.Mosaic;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
final class PeerUnmosaicer {

    /**
     * 公開可能な形から個体情報を復元する。
     * @param args 長さ 1。
     *            公開可能な形の個体情報
     * @throws MyRuleException 規約違反
     */
    public static void main(final String[] args) throws MyRuleException {
        final InetSocketAddress peer = Mosaic.peerFrom(args[0]);
        System.out.println(peer.getHostString() + " " + Integer.toString(peer.getPort()));
    }

}
