package nippon.kawauso.chiraura.network;

import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * CustomChord の転送先候補を保守する。
 * @author chirauraNoSakusha
 */
final class CcFingerStabilizer extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(CcFingerStabilizer.class.getName());

    // 参照。
    private final long interval;
    private final CcView view;
    private final BlockingQueue<NetworkTask> taskSink;

    CcFingerStabilizer(final BlockingQueue<Reporter.Report> reportSink, final long interval, final CcView view, final BlockingQueue<NetworkTask> taskSink) {
        super(reportSink);

        if (interval < 0) {
            throw new IllegalArgumentException("Negative interval ( " + interval + " ).");
        } else if (view == null) {
            throw new IllegalArgumentException("Null view.");
        } else if (taskSink == null) {
            throw new IllegalArgumentException("Null task sink.");
        }

        this.interval = interval;
        this.view = view;
        this.taskSink = taskSink;
    }

    @Override
    protected Void subCall() throws InterruptedException {
        final Random random = ThreadLocalRandom.current();
        while (!Thread.currentThread().isInterrupted()) {
            /*
             * 定期的に、転送先候補の中から 1 つ選んで接触要請を出す。
             */

            Thread.sleep(this.interval);

            final List<AddressedPeer> shortcuts = this.view.getFingers();

            if (shortcuts.isEmpty()) {
                continue;
            }

            final AddressedPeer target = shortcuts.get(random.nextInt(shortcuts.size()));
            ConcurrentFunctions.completePut(new PeerAccessRequest(target.getPeer()), this.taskSink);
            LOG.log(Level.FINER, "近道個体 {0} の確認要請を出しました。", target);
        }

        return null;
    }
}
