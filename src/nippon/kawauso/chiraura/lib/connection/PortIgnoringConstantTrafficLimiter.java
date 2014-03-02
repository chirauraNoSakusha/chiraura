/**
 * 
 */
package nippon.kawauso.chiraura.lib.connection;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author chirauraNoSakusha
 */
public final class PortIgnoringConstantTrafficLimiter extends ConstantTrafficLimiter<InetAddress> implements TrafficLimiter {

    /**
     * 作成する。
     * @param duration 単位監視期間 (ミリ秒)
     * @param sizeLimit 制限する通信量 (バイト)
     * @param countLimit 制限する通信回数
     * @param penalty 制限量に達したときの追加の待ち時間 (ミリ秒)
     */
    public PortIgnoringConstantTrafficLimiter(final long duration, final long sizeLimit, final int countLimit, final long penalty) {
        super(duration, sizeLimit, countLimit, penalty);
    }

    @Override
    public long nextSleep(final long size, final InetSocketAddress destination) throws InterruptedException {
        return super.nextSleep(size, destination.getAddress());
    }

    @Override
    public long nextSleep(final InetSocketAddress destination) throws InterruptedException {
        return super.nextSleep(destination.getAddress());
    }

    @Override
    public void remove(final InetSocketAddress destination) throws InterruptedException {
        super.remove(destination.getAddress());
    }

}
