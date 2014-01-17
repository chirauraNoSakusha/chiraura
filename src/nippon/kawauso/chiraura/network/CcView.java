/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.net.InetSocketAddress;
import java.util.List;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.container.Pair;

/**
 * 1つの個体から見える CostomChord の構造。
 * @author chirauraNoSakusha
 */
interface CcView {

    /*
     * 個体は論理空間でのその個体の位置を基点として、基点からから次の個体の位置の手前までを領土とする。
     * 任意の論理位置へ到達するために、基点から次の個体を把握する。
     * 機構への個体の出入りがあっても、確実に到達できるように、基点から次の個体だけでなく、
     * その次の個体、さらにその次の個体、さらにさらにその次の個体、と個体数の LOG の数の個体を把握しておく。
     * 任意の論理位置に高速で到達するために、基点から 2 のべき乗だけ離れた位置を領土とする個体を把握する。
     */

    /**
     * 基点を返す
     * @return 基点
     */
    Address getBase();

    /**
     * 基点個体の支配域を返す。
     * @return 支配域の先頭と末尾の論理位置
     */
    Pair<Address, Address> getDomain();

    /**
     * 登録されている個体が無いか検査。
     * @return 個体が無い場合のみ true
     */
    public boolean isEmpty();

    /**
     * 論理位置が基点個体の領土かどうか検査する。
     * @param target 調べる論理位置
     * @return 領土の場合のみ true
     */
    boolean dominates(final Address target);

    /**
     * 論理位置に到達するための転送先個体を返す。
     * @param target 目標の論理位置
     * @return 転送先個体
     */
    AddressedPeer getRoutingDestination(final Address target);

    /**
     * 論理空間正方向の近傍個体を列挙する。
     * @param maxHop いくつ先の個体まで含むか
     * @return 近傍個体
     */
    List<AddressedPeer> getSuccessors(final int maxHop);

    /**
     * 論理空間負方向の近傍個体を列挙する。
     * @param maxHop いくつ先の個体まで含むか
     * @return 近傍個体
     */
    List<AddressedPeer> getPredecessors(final int maxHop);

    /**
     * 転送先候補になる個体を列挙する。
     * @return 転送先候補の個体
     */
    List<AddressedPeer> getFingers();

    /**
     * 個体を列挙する。
     * 列挙する個体は getSuccessors, getPredecessors, getFingers の和。
     * @return 把握している重要な個体
     */
    public List<AddressedPeer> getImportantPeers();

    /**
     * 把握している個体を列挙する。
     * @return 把握している個体
     */
    List<AddressedPeer> getPeers();

    /**
     * 個体間の論理距離の平均を推定する。
     * @return 推定した個体間の論理距離の平均
     */
    Address estimateAverageDistance();

    /**
     * 個体を加える。
     * @param peer 加える個体
     * @return 構造に変化がある場合のみ true
     */
    boolean addPeer(final AddressedPeer peer);

    /**
     * 個体を取り除く。
     * @param peer 外す個体
     * @return 構造に変化がある場合は peer の論理位置。
     *         変化が無い場合は null
     */
    Address removePeer(final InetSocketAddress peer);

}
