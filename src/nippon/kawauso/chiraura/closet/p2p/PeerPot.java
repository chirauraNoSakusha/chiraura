package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 個体の掃き溜め。
 * @author chirauraNoSakusha
 */
interface PeerPot {

    /**
     * 個体の一つを得る。
     * @return 個体の一つ。
     *         空の場合 null
     */
    InetSocketAddress get();

    /**
     * 個体を入れる。
     * @param peer 入れる個体
     */
    void put(InetSocketAddress peer);

    /**
     * 入っている全個体を返す。
     * @return 入っている全個体
     */
    List<InetSocketAddress> getAll();

}
