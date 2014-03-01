/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;

/**
 * @author chirauraNoSakusha
 */
final class BasicConstantTrafficLimiter extends ConstantTrafficLimiter<InetSocketAddress> implements TrafficLimiter {

    BasicConstantTrafficLimiter(final long duration, final long sizeLimit, final int countLimit, final long penalty) {
        super(duration, sizeLimit, countLimit, penalty);
    }

    @Override
    public long nextSleep(final long size, final InetSocketAddress destination) throws InterruptedException {
        return super.nextSleep(size, destination);
    }

    @Override
    public long nextSleep(final InetSocketAddress destination) throws InterruptedException {
        return super.nextSleep(destination);
    }

    @Override
    public void remove(final InetSocketAddress destination) throws InterruptedException {
        super.remove(destination);
    }

}
