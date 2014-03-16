package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;

/**
 * 目障りな個体の通報。
 * @author chirauraNoSakusha
 */
final class OutlawReport {

    private final InetSocketAddress outlaw;

    OutlawReport(final InetSocketAddress outlaw) {
        this.outlaw = outlaw;
    }

    InetSocketAddress getOutlaw() {
        return this.outlaw;
    }

}
