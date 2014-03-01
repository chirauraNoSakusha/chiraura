/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author chirauraNoSakusha
 */
final class PortIgnoringConstantTrafficLimiter extends ConstantTrafficLimiter<InetAddress> implements TrafficLimiter {

    PortIgnoringConstantTrafficLimiter(final long duration, final long sizeLimit, final int countLimit, final long penalty) {
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
