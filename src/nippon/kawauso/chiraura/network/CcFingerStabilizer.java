/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

            final Map<Integer, Address> targets = new HashMap<>();
            for (final AddressedPeer peer : shortcuts) {
                final Address distance = this.view.getBase().distanceTo(peer.getAddress());
                targets.put(CcFunctions.distanceLevel(distance), distance);
            }

            final int[] levels = new int[targets.size()];
            final int[] weights = new int[targets.size()];
            final BigInteger averageDistance = this.view.estimateAverageDistance().toBigInteger();
            int index = 0;
            for (final Map.Entry<Integer, Address> entry : targets.entrySet()) {
                levels[index] = entry.getKey();
                final Address milestone = Address.ZERO.addPowerOfTwo(entry.getKey());
                final BigInteger diff = entry.getValue().distanceTo(milestone).toBigInteger();
                weights[index] = 1 + diff.divide(averageDistance).intValue();

                index++;
            }

            int weightSum = 0;
            for (final int weight : weights) {
                weightSum += weight;
            }
            int targetLevel = levels[0];
            int v = random.nextInt(weightSum);
            for (int i = 0; i < levels.length; i++) {
                v -= weights[i];
                if (v < 0) {
                    targetLevel = levels[i];
                    break;
                }
            }

            final Address target;
            if (targetLevel >= Address.SIZE) {
                // 2 の Address.SIZE 乗先は自分なので、その手前にする。
                target = this.view.getBase().subtractOne();
            } else {
                target = this.view.getBase().addPowerOfTwo(targetLevel);
            }

            ConcurrentFunctions.completePut(new AddressAccessRequest(target), this.taskSink);
            LOG.log(Level.FINER, "論理位置 {0} への近道の確認要請を出しました。", target);
        }

        return null;
    }
}
