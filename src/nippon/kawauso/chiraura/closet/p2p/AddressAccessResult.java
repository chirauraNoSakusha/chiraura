/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.List;

import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * 論理位置の支配者確認の結果。
 * 1. 支配者と支配者の把握する個体一覧。自分が支配者だった場合、支配者 は null。
 * 2. 諦め。
 */
final class AddressAccessResult {

    private final boolean givenUp;
    private final AddressedPeer manager;
    private final List<AddressedPeer> peers;

    private AddressAccessResult(final boolean givenUp, final AddressedPeer manager, final List<AddressedPeer> peers) {
        if (!givenUp && peers == null) {
            throw new IllegalArgumentException("Null peers.");
        }
        this.givenUp = givenUp;
        this.manager = manager;
        this.peers = peers;
    }

    AddressAccessResult(final AddressedPeer manager, final List<AddressedPeer> peers) {
        this(false, manager, peers);
    }

    static AddressAccessResult newGiveUp() {
        return new AddressAccessResult(true, null, null);
    }

    boolean isGivenUp() {
        return this.givenUp;
    }

    AddressedPeer getManager() {
        return this.manager;
    }

    List<AddressedPeer> getPeers() {
        return this.peers;
    }

}