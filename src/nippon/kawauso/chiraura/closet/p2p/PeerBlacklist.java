package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;

/**
 * 除外対象個体一覧。
 * @author chirauraNoSakusha
 */
interface PeerBlacklist {

    /**
     * 個体が含まれているか検査。
     * @param peer 検査する個体
     * @return 含まれている場合のみ true
     */
    boolean contains(InetSocketAddress peer);

    /**
     * 個体を加える。
     * @param peer 加える個体
     * @return 新しく加えられた場合のみ true
     */
    boolean add(InetSocketAddress peer);

    /**
     * 個体を取り除く。
     * @param peer 取り除く個体
     * @return 実際に取り除かれた場合のみ true
     */
    boolean remove(InetSocketAddress peer);

}
