/**
 * 
 */
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
 * CustomChord のご近所さんを保守する。
 * @author chirauraNoSakusha
 */
final class CcSuccessorStabilizer extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(CcSuccessorStabilizer.class.getName());

    // 参照。
    private final long interval;
    private final CcView view;
    private final BlockingQueue<NetworkTask> taskSink;

    CcSuccessorStabilizer(final BlockingQueue<Reporter.Report> reportSink, final long interval, final CcView view, final BlockingQueue<NetworkTask> taskSink) {
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
    protected Void subCall() throws Exception {
        final Random random = ThreadLocalRandom.current();
        while (!Thread.currentThread().isInterrupted()) {
            /*
             * ご近所さんの中から1つを選んで接触要請を出す。
             */
            final List<AddressedPeer> successors = this.view.getSuccessors(Integer.MAX_VALUE);

            if (!successors.isEmpty()) {
                final AddressedPeer target = successors.get(random.nextInt(successors.size()));
                ConcurrentFunctions.completePut(new PeerAccessRequest(target.getPeer()), this.taskSink);
                LOG.log(Level.FINER, "後続個体 {0} の確認要請を出しました。", target);
            }

            Thread.sleep(this.interval);
        }

        return null;
    }

}
