package nippon.kawauso.chiraura.network;

import java.util.List;

import nippon.kawauso.chiraura.lib.base.Address;


/**
 * CustomChord での通信先候補となる個体 (への距離)。
 * @author chirauraNoSakusha
 */
interface CcShortcutTable {

    /**
     * 空かどうか検査
     * @return 空の場合のみ true
     */
    boolean isEmpty();

    /**
     * 論理位置に到達するために次に通信すべき個体を返す。
     * @param targetDistance 調べる論理位置までの距離
     * @return 次に通信すべき個体への距離
     */
    Address getRoutingDestination(final Address targetDistance);

    /**
     * 通信先候補の全ての個体への距離を返す。
     * @return 通信先候補の全ての個体への距離
     */
    List<Address> getAll();

    /**
     * 個体を加える。
     * @param peerDistance 加える個体への距離
     * @return 新しく通信先候補として加えられた場合のみ true
     */
    boolean add(final Address peerDistance);

    /**
     * 個体を外す。
     * @param peerDistance 外す個体への距離
     * @return 外す個体が通信先候補だったときのみ true
     */
    boolean remove(final Address peerDistance);

    /**
     * 空にする。
     */
    void clear();

}