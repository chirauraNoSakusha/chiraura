/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * 寂しがり。
 * 接続個体がいない場合、てきとうに接続を試みる。
 * @author chirauraNoSakusha
 */
final class Lonely extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(Lonely.class.getName());

    private static final int MAX_SLAVES = 5;

    // 参照。
    private final NetworkWrapper network;
    private final ExecutorService executor;
    private final long minInterval;
    private final long timeout;
    private final FirstAccessSelectDriver driver;

    private final long maxInterval;

    Lonely(final BlockingQueue<Report> reportSink, final NetworkWrapper network, final ExecutorService executor, final long minInterval,
            final long maxInterval, final long timeout, final FirstAccessSelectDriver driver) {
        super(reportSink);

        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        } else if (minInterval < 0) {
            throw new IllegalArgumentException("Negative min interval ( " + minInterval + " ).");
        } else if (maxInterval < minInterval) {
            throw new IllegalArgumentException("Too small max interval ( " + maxInterval + " ) < min interval ( " + minInterval + " ).");
        } else if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout ( " + timeout + " ).");
        } else if (driver == null) {
            throw new IllegalArgumentException("Null driver.");
        }
        this.network = network;
        this.executor = executor;
        this.minInterval = minInterval;
        this.maxInterval = maxInterval;
        this.timeout = timeout;
        this.driver = driver;
    }

    private static void cleanUp(final Set<Future<Void>> slaves) throws InterruptedException {
        for (final Iterator<Future<Void>> iterator = slaves.iterator(); iterator.hasNext();) {
            final Future<Void> future = iterator.next();
            boolean end = false;
            try {
                future.get(0, TimeUnit.MILLISECONDS);
                end = true;
            } catch (final TimeoutException ignored) {
            } catch (final ExecutionException e) {
                end = true;
            }
            if (end) {
                iterator.remove();
            }
        }
    }

    @Override
    protected Void subCall() throws InterruptedException {
        /*
         * 接続個体がいなかったら、接続を試みる。
         */

        final Set<InetSocketAddress> tried = new HashSet<>();
        final Set<Future<Void>> slaves = new HashSet<>();

        long interval = this.minInterval;
        while (!Thread.currentThread().isInterrupted()) {
            cleanUp(slaves);

            if (slaves.size() >= MAX_SLAVES) {
                // 目一杯働いてるなら小休止。
                Thread.sleep(interval / MAX_SLAVES);
            } else if (!this.network.isEmpty()) {
                // 既に接続しているなら安心して休む。
                interval = this.minInterval;
                tried.clear();
                Thread.sleep(interval);
            } else {

                final InetSocketAddress peer = this.network.getReservedPeer();

                if (peer == null) {
                    // 接続候補が無いなら寝て待つ。
                    interval = this.minInterval;
                    tried.clear();
                    Thread.sleep(interval);
                } else if (tried.contains(peer)) {
                    // 接続候補が一巡してたら 1 回休み。
                    final long nextInterval = Math.min(interval * 2, this.maxInterval);
                    if (nextInterval > interval) {
                        LOG.log(Level.FINER, "少し手を抜くか ( {0} )。", Long.toString(nextInterval));
                        interval = nextInterval;
                    }
                    tried.clear();
                    Thread.sleep(interval / MAX_SLAVES);
                } else if (this.network.inBlacklist(peer)) {
                    // 拒否対象の場合は飛ばす。
                    tried.add(peer);
                } else {
                    tried.add(peer);
                    LOG.log(Level.FINER, "{0} に声を掛けます。", peer);
                    slaves.add(this.executor.submit(new Reporter<Void>(Level.WARNING) {
                        @Override
                        protected Void subCall() throws InterruptedException {
                            Lonely.this.driver.execute(new FirstAccessOperation(peer), Lonely.this.timeout);
                            return null;
                        }
                    }));
                    Thread.sleep(interval / MAX_SLAVES);
                }
            }
        }

        return null;
    }

}
