package nippon.kawauso.chiraura.network;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.container.Pair;

/**
 * IPネットワーク上に作った分散メモリっぽいもの。
 * @author chirauraNoSakusha
 */
public interface AddressableNetwork {

    /**
     * 自分の論理位置を返す。
     * @return 自分の論理位置
     */
    public Address getSelf();

    /**
     * 領土を返す。
     * @return 領土の先頭と末尾の組
     */
    public Pair<Address, Address> getDomain();

    /**
     * 登録されている個体が無いか検査。
     * @return 個体が無い場合のみ true
     */
    public boolean isEmpty();

    /**
     * 論理位置が自分の領土かどうか検査する。
     * @param target 検査する論理位置
     * @return 自分の領土の場合のみ true
     */
    public boolean dominates(Address target);

    /**
     * 比較対象より自分の方が論理位置の支配者に近いかどうかを検査する。
     * @param target 検査する論理位置
     * @param competitor 比較対象の論理位置
     * @return 自分の方が支配者に近い場合のみ true
     */
    public boolean moreAppropriate(Address target, Address competitor);

    /**
     * 論理位置に辿り着くための転送先となる個体を返す。
     * @param target 目的の論理位置
     * @return 転送先の個体
     */
    public AddressedPeer getRoutingDestination(Address target);

    /**
     * 経路維持に関わる近傍個体を列挙する。
     * @param maxHop いくつ先の個体まで含むか
     * @return 近傍個体
     */
    public List<AddressedPeer> getRoutingNeighbors(int maxHop);

    /**
     * データ維持に関わる近傍個体を列挙する。
     * @param maxHop いくつ先の個体まで含むか
     * @return 近傍個体
     */
    public List<AddressedPeer> getBackupNeighbors(int maxHop);

    /**
     * 転送先候補になる個体を列挙する。
     * @return 転送先候補の個体
     */
    public List<AddressedPeer> getShortcuts();

    /**
     * 個体を列挙する。
     * 列挙する個体は getRoutingNeighbors, getBackupNeighbors, getShortcuts の和。
     * @return 把握している重要な個体
     */
    public List<AddressedPeer> getImportantPeers();

    /**
     * 個体を列挙する。
     * @return 把握している個体
     */
    public List<AddressedPeer> getPeers();

    /**
     * 個体を加える。
     * @param peer 加える個体
     * @return 構造に加えられた場合のみ true
     */
    public boolean addPeer(AddressedPeer peer);

    /**
     * 個体を取り除く。
     * @param peer 取り除く個体
     * @return 構造から削除された場合はその論理位置。
     *         そうでなければ null
     */
    public Address removePeer(InetSocketAddress peer);

    /**
     * 能動的維持活動を始める。
     * @param executor 実行機
     */
    public void start(ExecutorService executor);

    /**
     * 接続構造の管理に必要な仕事を取り出す。
     * 仕事が無いときは発生するまで待つ。
     * @return 最も古い仕事。
     * @throws InterruptedException 割り込まれた場合
     */
    public NetworkTask take() throws InterruptedException;

    /**
     * 接続構造の管理に必要な仕事を取り出す。
     * @return 最も古い仕事。
     *         受信したメッセージが無いときは null
     */
    public NetworkTask takeIfExists();

}
