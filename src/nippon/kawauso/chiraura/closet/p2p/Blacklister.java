package nippon.kawauso.chiraura.closet.p2p;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.connection.Limiter;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * @author chirauraNoSakusha
 */
final class Blacklister extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(Blacklister.class.getName());

    private final BlockingQueue<OutlawReport> outlawReportSource;
    private final Limiter<InetSocketAddress> limiter;
    private final NetworkWrapper network;
    private final Set<InetSocketAddress> removers; // 並列処理対応とする。
    private final ExecutorService executor;

    Blacklister(final BlockingQueue<? super Reporter.Report> reportSink, final BlockingQueue<OutlawReport> outlawReportSource,
            final Limiter<InetSocketAddress> limiter, final NetworkWrapper network, final Set<InetSocketAddress> removers, final ExecutorService executor) {
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

            if (this.removers.contains(report.getOutlaw())) {
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
                         * そのため、本当に資源が解放されているかもう一度 limiter.remove で確認する。
                         * これで、資源が確保されているのに remover が居ない状態は避けられる。
                         * しかし、上の removers.remove と下の removers.add の間に removers.contains され、remover が重複する可能性はある。
                         * ただし、下の limiter.remove で待機するのは、上の limiter.remove と下の limter.remove の間に
                         * limiter.addValueAndCheckPenalty された場合だけだから、そんなに頻繁に起こることは無いはず。
                         */

                        if (Blacklister.this.limiter.remove(report.getOutlaw())) {
                            LOG.log(Level.FINER, "{0} を監視対象から除外しました。", report.getOutlaw());
                            break;
                        }

                        Blacklister.this.removers.add(report.getOutlaw());
                    }
                    return null;
                }
            });
            LOG.log(Level.FINER, "{0} を監視対象に加えました。", report.getOutlaw());
        }

        return null;
    }

}
