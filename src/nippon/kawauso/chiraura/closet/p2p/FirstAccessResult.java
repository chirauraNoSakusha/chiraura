package nippon.kawauso.chiraura.closet.p2p;

import java.util.List;

import nippon.kawauso.chiraura.network.AddressedPeer;

/**
 * @author chirauraNoSakusha
 */
final class FirstAccessResult {

    private final boolean giveUp;

    private final List<AddressedPeer> peers;

    private FirstAccessResult(final boolean giveUp, final List<AddressedPeer> peers) {
        this.giveUp = giveUp;
        this.peers = peers;
    }

    static FirstAccessResult newGiveUp() {
        return new FirstAccessResult(true, null);
    }

    FirstAccessResult(final List<AddressedPeer> peers) {
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
