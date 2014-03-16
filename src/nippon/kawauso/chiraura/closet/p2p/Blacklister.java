package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.connection.Limiter;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * blacklist を司る最強の下っ端。
 * @author chirauraNoSakusha
 */
final class Blacklister extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(Blacklister.class.getName());

    private final BlockingQueue<OutlawReport> outlawReportSource;
    private final Limiter<InetSocketAddress> limiter;
    private final NetworkWrapper network;
    private final ConcurrentMap<InetSocketAddress, Boolean> removers; // 並列処理対応とする。
    private final ExecutorService executor;

    Blacklister(final BlockingQueue<? super Reporter.Report> reportSink, final BlockingQueue<OutlawReport> outlawReportSource,
            final Limiter<InetSocketAddress> limiter, final NetworkWrapper network, final ConcurrentMap<InetSocketAddress, Boolean> removers,
            final ExecutorService executor) {
        super(reportSink);

        if (outlawReportSource == null) {
            throw new IllegalArgumentException("Null outlaw report source.");
        } else if (limiter == null) {
            throw new IllegalArgumentException("Null limiter.");
        } else if (network == null) {
            throw new IllegalArgumentException("Null network.");
        } else if (removers == null) {
            throw new IllegalArgumentException("Null removers.");
        } else if (executor == null) {
            throw new IllegalArgumentException("Null executor.");
        }
        this.outlawReportSource = outlawReportSource;
        this.limiter = limiter;
        this.network = network;
        this.removers = removers;
        this.executor = executor;
    }

    @Override
    protected Void subCall() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            final OutlawReport report = this.outlawReportSource.take();

            final long penalty = this.limiter.addValueAndCheckPenalty(report.getOutlaw(), 0); // 見るのは回数だけ。

            if (penalty > 0) {
                this.network.removeInvalidPeer(report.getOutlaw());
                LOG.log(Level.FINER, "{0} を拒否対象に加えました。", report.getOutlaw());
            }

            final Boolean exists = this.removers.putIfAbsent(report.getOutlaw(), true);
            if (exists != null) {
                // 既に対応する remover がいる。
                continue;
            }

            this.executor.submit(new Reporter<Void>(Level.WARNING) {
                @Override
                protected Void subCall() throws InterruptedException {
                    while (!Thread.currentThread().isInterrupted()) {
                        while (!Blacklister.this.limiter.remove(report.getOutlaw())) {
                        }

                        Blacklister.this.removers.remove(report.getOutlaw());

                        /*
                         * 上の limiter.remove と上の removers.remove の間に limiter.addValueAndCheckPenalty が行われ得る。
                         * そのため、本当に資源が解放されているかもう一度確認する。
                         * これで、資源が確保されているのに remover が居ない状態は避けられる。
                         */

                        if (Blacklister.this.limiter.checkCount(report.getOutlaw()) == 0) {
                            LOG.log(Level.FINER, "{0} を監視対象から除外しました。", report.getOutlaw());
                            break;
                        }

                        final Boolean exists2 = Blacklister.this.removers.putIfAbsent(report.getOutlaw(), true);
                        if (exists2 != null) {
                            // 既に別の remover がいる。
                            break;
                        }
                    }
                    return null;
                }
            });
            LOG.log(Level.FINER, "{0} を監視対象に加えました。", report.getOutlaw());
        }

        return null;
    }

}
