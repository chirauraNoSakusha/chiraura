/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;

/**
 * @author chirauraNoSakusha
 */
public final class SelfReport implements MessengerReport {

    private final InetSocketAddress self;

    SelfReport(final InetSocketAddress self) {
        if (self == null) {
            throw new IllegalArgumentException("Null self.");
        }
        this.self = self;
    }

    /**
     * 自分の IP アドレスを返す。
     * @return 自分の IP アドレス
     */
    public InetSocketAddress get() {
        return this.self;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.self)
                .append(']').toString();
    }

}
