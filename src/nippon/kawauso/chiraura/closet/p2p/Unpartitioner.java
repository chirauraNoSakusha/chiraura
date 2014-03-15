package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * ネットワーク分断を防止する。
 * たまに接続していない個体に接続を試みる。
 * こいつがいないと、初期個体が X しか無い場合、X が一旦落ちて、X 抜きのネットワーク N1 ができた後、
 * X が復帰し、すぐに N1 を知らない個体 Y が X に接続すると、X と Y のネットワーク N2 ができ、
 * 新しい個体は N2 に入っていくようになってしまう。
 * @author chirauraNoSakusha
 */
final class Unpartitioner extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(Unpartitioner.class.getName());

    // 参照。
    private final NetworkWrapper network;
    private final long interval;
    private final long timeout;
    private final FirstAccessSelectDriver driver;

    Unpartitioner(final BlockingQueue<Report> reportSink, final NetworkWrapper network, final long interval, final long timeout,
            final FirstAccessSelectDriver driver) {
        super(reportSink);

        if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (interval < 0) {
            throw new IllegalArgumentException("Negative min interval ( " + interval + " ).");
        } else if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout ( " + timeout + " ).");
        } else if (driver == null) {
            throw new IllegalArgumentException("Null driver.");
        }
        this.network = network;
        this.interval = interval;
        this.timeout = timeout;
        this.driver = driver;
    }

    @Override
    protected Void subCall() throws InterruptedException {
        /*
         * 接続していない個体に個体に接続を試みる。
         */

        while (!Thread.currentThread().isInterrupted()) {
            if (this.network.isEmpty()) {
                // まだ誰とも接続していないなら安心して休む。今は Lonely の時間。
                Thread.sleep(this.interval);
                continue;
            }

            final InetSocketAddress peer = this.network.getReservedPeer();

            if (peer == null) {
                // 接続候補が無いなら寝て待つ。
                Thread.sleep(this.interval);
            } else if (this.network.containsConnection(peer)) {
                // 既に接続している相手なら寝る。
                Thread.sleep(this.interval);
            } else if (this.network.inBlacklist(peer)) {
                // 拒否対象の場合は飛ばす。
                Thread.sleep(this.interval);
            } else {
                LOG.log(Level.FINER, "{0} に声を掛けます。", peer);

                final long start = System.currentTimeMillis();
                Unpartitioner.this.driver.execute(new FirstAccessOperation(peer), Unpartitioner.this.timeout);
                final long end = System.currentTimeMillis();

                if (start + this.interval > end) {
                    Thread.sleep(start + this.interval - end);
                }
            }
        }

        return null;
    }
}
