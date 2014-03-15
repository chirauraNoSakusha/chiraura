package nippon.kawauso.chiraura.closet.p2p;

import java.util.List;

import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * 個体確認の結果。
 * 1. 個体一覧。
 * 2. 諦め。
 * @author chirauraNoSakusha
 */
final class PeerAccessResult {

    private final boolean giveUp;

    private final List<AddressedPeer> peers;

    private PeerAccessResult(final boolean giveUp, final List<AddressedPeer> peers) {
        this.giveUp = giveUp;
        this.peers = peers;
    }

    static PeerAccessResult newGiveUp() {
        return new PeerAccessResult(true, null);
    }

    PeerAccessResult(final List<AddressedPeer> peers) {
        this(false, peers);
        if (peers == null) {
            throw new IllegalArgumentException("Null peers.");
        }
    }

    boolean isGivenUp() {
        return this.giveUp;
    }

    List<AddressedPeer> getPeers() {
        return this.peers;
    }

}
