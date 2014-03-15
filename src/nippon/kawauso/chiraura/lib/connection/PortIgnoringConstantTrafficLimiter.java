package nippon.kawauso.chiraura.lib.connection;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author chirauraNoSakusha
 */
public final class PortIgnoringConstantTrafficLimiter implements Limiter<InetSocketAddress> {

    private final ConstantLimiter<InetAddress> base;

    /**
     * 作成する。
     * @param duration 単位監視期間 (ミリ秒)
     * @param sizeLimit 制限する通信量 (バイト)
     * @param countLimit 制限する通信回数
     * @param penalty 制限量に達したときの追加の待ち時間 (ミリ秒)
     */
    public PortIgnoringConstantTrafficLimiter(final long duration, final long sizeLimit, final int countLimit, final long penalty) {
        this.base = new ConstantLimiter<InetAddress>(duration, sizeLimit, countLimit, penalty) {};
    }

    @Override
    public long addValueAndCheckPenalty(final InetSocketAddress destination, final long size) throws InterruptedException {
        return this.base.addValueAndCheckPenalty(destination.getAddress(), size);
    }

    @Override
    public long checkPenalty(final InetSocketAddress destination) throws InterruptedException {
        return this.base.checkPenalty(destination.getAddress());
    }

    @Override
    public void remove(final InetSocketAddress destination) throws InterruptedException {
        this.base.remove(destination.getAddress());
    }

}
