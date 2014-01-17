/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.net.InetSocketAddress;

/**
 * 個体への接触要請。
 * @author chirauraNoSakusha
 */
public final class PeerAccessRequest implements NetworkTask {

    private final InetSocketAddress peer;

    PeerAccessRequest(final InetSocketAddress peer) {
        if (peer == null) {
            throw new IllegalArgumentException("Null peer.");
        }
        this.peer = peer;
    }

    /**
     * 接触して欲しい個体を返す。
     * @return 接触して欲しい個体
     */
    public InetSocketAddress getPeer() {
        return this.peer;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.peer)
                .append(']').toString();
    }

}
