package nippon.kawauso.chiraura.messenger;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author chirauraNoSakusha
 */
public final class SelfReport implements MessengerReport {

    private final InetSocketAddress self;
    private final InetAddress destination;

    SelfReport(final InetSocketAddress self, final InetAddress destination) {
        if (self == null) {
            throw new IllegalArgumentException("Null self.");
        } else if (destination == null) {
            throw new IllegalArgumentException("Null destination.");
        }
        this.self = self;
        this.destination = destination;
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
    public InetAddress getDestination() {
        return this.destination;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.self)
                .append(']').toString();
    }

}
