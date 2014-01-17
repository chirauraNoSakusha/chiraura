/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.util.List;

import nippon.kawauso.chiraura.lib.base.Address;


/**
 * CustomChord での論理空間正方向のご近所さん(への距離)。
 * @author chirauraNoSakusha
 */
interface CcNeighborList {

    /**
     * 空かどうか検査する。
     * @return 空の場合のみ true
     */
    boolean isEmpty();

    /**
     * 現在の容量を返す。
     * @return 現在の容量
     */
    int getCurrentCapacity();

    /**
     * 隣りの個体への距離を返す。
     * @return 隣りの個体への距離。
     *         空の場合は null
     */
    Address getNeighbor();

    /**
     * 近所で一番遠い個体への距離を返す。
     * @return 近所で一番遠い個体への距離。
     *         空の場合は null
     */
    Address getFarthestNeighbor();

    /**
     * 近所の個体への距離を返す。
     * @param maxHop 最大で何ホップ先の近所まで含めるか
     * @return 近所の個体への距離
     */
    List<Address> getNeighbors(final int maxHop);

    /**
     * 近所の全ての個体への距離を返す。
     * @return 近所の全ての個体への距離
     */
    List<Address> getAll();

    /**
     * 個体間の論理距離の平均を返す。
     * @return 個体間の論理距離
     */
    Address getAverageDistance();

    /**
     * 個体を加える。
     * @param peerDistance 加える個体への距離
     * @return 新しくご近所さんとして加えられた場合のみ true
     */
    boolean add(final Address peerDistance);

    /**
     * 個体を外す。
     * @param peerDistance 外す個体への距離
     * @return 外す個体がご近所さんだった場合のみ true
     */
    boolean remove(final Address peerDistance);

    /**
     * 空にする。
     */
    void clear();

}