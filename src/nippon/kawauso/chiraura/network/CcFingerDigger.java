/**
 * 
 */
package nippon.kawauso.chiraura.network;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
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

            final BigInteger averageDistance = this.view.estimateAverageDistance().toBigInteger();

            // レベルごとに、そのレベルの最大距離からのズレとピア間平均距離の比を重みにする。
            final List<Integer> levels = new ArrayList<>();
            final List<Integer> weights = new ArrayList<>();

            int preLevel = -1;
            Address preDistance = null;
            for (final AddressedPeer peer : shortcuts) {
                final Address distance = this.view.getBase().distanceTo(peer.getAddress());
                final int level = CcFunctions.distanceLevel(distance);
                if (preDistance != null && level > preLevel + 1) {
                    for (int i = preLevel + 1; i < level; i++) {
                        final Address milestone = Address.ZERO.addPowerOfTwo(i);
                        final BigInteger diff = preDistance.distanceTo(milestone).toBigInteger();
                        levels.add(i);
                        weights.add(1 + diff.divide(averageDistance).intValue());
                        // System.out.println("Aho " + i + " " + averageDistance + " " + diff);
                    }
                }
                final Address milestone = Address.ZERO.addPowerOfTwo(level);
                final BigInteger diff = distance.distanceTo(milestone).toBigInteger();
                levels.add(level);
                weights.add(1 + diff.divide(averageDistance).intValue());
                // System.out.println("Baka " + level + " " + averageDistance + " " + diff);

                preLevel = level;
                preDistance = distance;
            }

            for (int i = preLevel + 1; i <= Address.SIZE; i++) {
                final Address milestone = Address.ZERO.addPowerOfTwo(i);
                @SuppressWarnings("null")
                // shortcuts が空ではないので大丈夫。
                final BigInteger diff = preDistance.distanceTo(milestone).toBigInteger();
                levels.add(i);
                weights.add(1 + diff.divide(averageDistance).intValue());
                // System.out.println("China " + i + " " + averageDistance + " " + diff);
            }

            int weightSum = 0;
            for (final int weight : weights) {
                weightSum += weight;
            }
            int targetLevel = levels.get(0);
            int v = random.nextInt(weightSum);
            for (int i = 0; i < levels.size(); i++) {
                v -= weights.get(i);
                if (v < 0) {
                    targetLevel = levels.get(i);
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

            // for (int i = 0; i < levels.size(); i++) {
            // System.out.println(levels.get(i) + " " + weights.get(i));
            // }

            ConcurrentFunctions.completePut(new AddressAccessRequest(target), this.taskSink);
            LOG.log(Level.FINER, "論理位置 {0} への近道の開拓要請を出しました。", target);
        }

        return null;
    }
}
