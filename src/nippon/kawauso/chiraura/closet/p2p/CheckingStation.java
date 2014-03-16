package nippon.kawauso.chiraura.closet.p2p;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 目的が同じ仕事人を1人しか通さない関所。
 * @author chirauraNoSakusha
 */
final class CheckingStation<K, V> {

    // private static final Logger LOG = Logger.getLogger(CheckingStation.class.getName());

    /**
     * 結果取得用の手形。
     * @author chirauraNoSakusha
     */
    static final class Instrument<T> {

        private final CountDownLatch barrier;
        private T result;

        private Instrument() {
            this.barrier = new CountDownLatch(1);
            this.result = null;
        }

        /**
         * 結果を取得する。
         * 返り値が null の場合、時間切れの場合と null が設定された場合の両方があり得る。
         * @param timeout 待つ時間
         * @return 結果。
         *         時間切れの場合 null
         * @throws InterruptedException 割り込まれた場合
         */
        T get(final long timeout) throws InterruptedException {
            if (this.barrier.await(timeout, TimeUnit.MILLISECONDS)) {
                return this.result;
            } else {
                return this.result;
            }
        }

        /**
         * 結果を通知する。
         * @param result 結果
         */
        private void set(final T result) {
            this.result = result;
            this.barrier.countDown();
        }

    }

    private final ConcurrentMap<K, Instrument<V>> container;

    CheckingStation() {
        this.container = new ConcurrentHashMap<>();
    }

    /**
     * 通る。
     * @param key 目的
     * @return 通れたら null。
     *         通れなかったら、結果取得用の手形
     */
    Instrument<V> register(final K key) {
        /*
         * return this.container.putIfAbsent(key, new Instrument<V>());
         * だけでも正常に動く。
         */
        final Instrument<V> instrument = this.container.get(key);
        if (instrument != null) {
            return instrument;
        } else {
            return this.container.putIfAbsent(key, new Instrument<V>());
        }
    }

    /**
     * 結果を渡して関所を開ける。
     * @param key 目的
     * @param value 結果
     * @return 開けたら true。
     *         元から開いてたら false
     */
    boolean free(final K key, final V value) {
        final Instrument<V> instrument = this.container.remove(key);
        if (instrument != null) {
            instrument.set(value);
            return true;
        } else {
            return false;
        }
    }

}
