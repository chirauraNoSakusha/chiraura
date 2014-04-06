package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;

import nippon.kawauso.chiraura.closet.ClosetReport;

/**
 * @author chirauraNoSakusha
 */
public final class SelfReport implements ClosetReport {

    private final InetSocketAddress self;
    private final InetSocketAddress destination;

    SelfReport(final nippon.kawauso.chiraura.messenger.SelfReport base) {
        if (base == null) {
            throw new IllegalArgumentException("Null base.");
        }
        this.self = base.getSelf();
        this.destination = base.getDestination();
    }

    /**
     * 自分の IP アドレスを返す。
     * @return 自分の IP アドレス
     */
    public InetSocketAddress getSelf() {
        return this.self;
    }

    /**
     * 通信先を返す。
     * @return 通信先
     */
    public InetSocketAddress getDestination() {
        return this.destination;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.self)
                .append(']').toString();
    }

}
