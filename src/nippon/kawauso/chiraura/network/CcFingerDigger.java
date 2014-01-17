/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.concurrent.ConcurrentFunctions;
import nippon.kawauso.chiraura.lib.process.Reporter;

/**
 * CustomChord の転送先候補を保守する。
 * @author chirauraNoSakusha
 */
final class CcFingerDigger extends Reporter<Void> {

    private static final Logger LOG = Logger.getLogger(CcFingerDigger.class.getName());

    // 参照。
    private final long interval;
    private final CcView view;
    private final BlockingQueue<NetworkTask> taskSink;

    CcFingerDigger(final BlockingQueue<Reporter.Report> reportSink, final long interval, final CcView view, final BlockingQueue<NetworkTask> taskSink) {
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
             * 定期的に、転送先候補の穴を選んで接触要請を出す。
             */

            Thread.sleep(this.interval);

            final List<AddressedPeer> shortcuts = this.view.getFingers();

            if (shortcuts.isEmpty()) {
                continue;
            }

            final int averageLevel = CcFunctions.distanceLevel(this.view.estimateAverageDistance());
            final Set<Integer> targetLevels = new HashSet<>();
            for (int i = averageLevel; i <= Address.SIZE; i++) {
                targetLevels.add(i);
            }

            for (final AddressedPeer peer : shortcuts) {
                targetLevels.remove(CcFunctions.distanceLevel(this.view.getBase().distanceTo(peer.getAddress())));
            }

            if (targetLevels.isEmpty()) {
                continue;
            }

            final int targetLevel = targetLevels.toArray(new Integer[0])[random.nextInt(targetLevels.size())];

            final Address target;
            if (targetLevel >= Address.SIZE) {
                // 2 の Address.SIZE 乗先は自分なので、その手前にする。
                target = this.view.getBase().subtractOne();
            } else {
                target = this.view.getBase().addPowerOfTwo(targetLevel);
            }

            ConcurrentFunctions.completePut(new AddressAccessRequest(target), this.taskSink);
            LOG.log(Level.FINER, "論理位置 {0} への近道の開拓要請を出しました。", target);
        }

        return null;
    }
}
