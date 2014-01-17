/**
 * 
 */
package nippon.kawauso.chiraura.lib;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author chirauraNoSakusha
 */
public final class ArrayFunctions {

    /**
     * 小さい方から n 個の要素を候補範囲の中から選び、候補範囲の前方に詰める。
     * 要素の構成が変わらないように、元々候補範囲の前方にあった要素は後方のどこかに移される。
     * @param <T> 要素の型
     * @param array 配列
     * @param n 選ぶ要素数
     * @param fromIndex 候補範囲の先頭
     * @param toIndex 候補範囲の末尾の 1 つ先
     * @param comparator 比較器
     */
    public static <T> void orderSelect(final T[] array, final int n, final int fromIndex, final int toIndex, final Comparator<T> comparator) {
        if (n < 0) {
            throw new IllegalArgumentException("number of selection ( " + n + " ) < 0.");
        } else if (fromIndex > toIndex) {
            throw new IllegalArgumentException("from index ( " + fromIndex + " ) > to index ( " + toIndex + " ).");
        } else if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException("form index ( " + fromIndex + " ) < 0.");
        } else if (array.length < toIndex) {
            throw new ArrayIndexOutOfBoundsException("array length ( " + array.length + " ) < to index ( " + toIndex + " ) .");
        }

        if (n == 0) {
            return;
        } else if (toIndex - fromIndex <= n) {
            // 全部選ぶ。
            return;
        }
        quickSelect(array, n, fromIndex, toIndex, comparator);
    }

    @SuppressWarnings("unused")
    private static <T> void naiveSelect(final T[] array, final int n, final int fromIndex, final int toIndex, final Comparator<T> comparator) {
        Arrays.sort(array, fromIndex, toIndex, comparator);
    }

    private static <T> void quickSelect(final T[] array, final int n, final int fromIndex, final int toIndex, final Comparator<T> comparator) {
        int from = fromIndex;
        int to = toIndex;
        int nSelected = 0;
        while (true) {
            /*
             * pivot 以下なら前方、pivot より大きければ後方に詰める。
             */
            final T pivot = array[from];
            /*
             * head より前は pivot 以下、
             * tail より後ろは pivot より大きいことが確定。
             */
            int head = from + 1;
            int tail = to - 1;
            while (head < tail) {
                if (comparator.compare(array[head], pivot) <= 0) {
                    head++;
                } else {
                    final T tmp = array[tail];
                    array[tail] = array[head];
                    array[head] = tmp;
                    tail--;
                }
            }
            // head == tail.
            if (comparator.compare(array[head], pivot) <= 0) {
                head++;
            } else {
                tail--;
            }

            /*
             * head == tail + 1 なので、
             * tail 以前は pivot 以下、
             * head 以降は pivot より大きいことが確定。
             */

            // pivot (前半の最大値) を tail (前半の末尾) に移動。
            array[from] = array[tail];
            array[tail] = pivot;

            // tail 以前を選択すると、
            final int diff = nSelected + head - from - n;
            if (diff < 0) {
                // まだ足りない。
                nSelected += head - from;
                from = head;
            } else if (diff > 0) {
                // 多過ぎた。
                if (diff == 1) {
                    // pivot (head より前の最大値) を抜かせば完了。
                    break;
                }
                to = tail;
            } else {
                // ぴったり完了。
                break;
            }
        }
    }

    /**
     * 無作為に n 個の要素を候補範囲の中から選び、候補範囲の前方に詰める。
     * 要素の構成が変わらないように、元々候補範囲の前方にあった要素は後方のどこかに移される。
     * @param <T> 要素の型
     * @param array 配列
     * @param n 選ぶ要素数
     * @param fromIndex 候補範囲の先頭
     * @param toIndex 候補範囲の末尾の 1 つ先
     */
    public static <T> void randomSelect(final T[] array, final int n, final int fromIndex, final int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("from index ( " + fromIndex + " ) > to index ( " + toIndex + " ).");
        } else if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException("form index ( " + fromIndex + " ) < 0.");
        } else if (array.length < toIndex) {
            throw new ArrayIndexOutOfBoundsException("array length ( " + array.length + " ) < to index ( " + toIndex + " ) .");
        }

        final int width = toIndex - fromIndex;
        if (width <= n) {
            // 全部選ぶ。
            return;
        }

        final Random random = ThreadLocalRandom.current();
        if (n <= width / 2) {
            // 入れるものを選ぶ。
            for (int i = 0; i < n; i++) {
                final int offset = random.nextInt(width - i);
                final T tmp = array[fromIndex + i];
                array[fromIndex + i] = array[fromIndex + offset];
                array[fromIndex + offset] = tmp;
            }
        } else {
            // 外すものを選ぶ。
            for (int i = 0; i < width - n; i++) {
                final int offset = random.nextInt(width - i);
                final T tmp = array[toIndex - 1 - i];
                array[toIndex - 1 - i] = array[toIndex - 1 - offset];
                array[toIndex - 1 - offset] = tmp;
            }
        }
    }

    /**
     * 要素を入れ替える。
     * @param <T> 要素の方
     * @param array 配列
     * @param index1 入れ替える位置
     * @param index2 入れ替える位置
     */
    public static <T> void swap(final T[] array, final int index1, final int index2) {
        final T tmp = array[index1];
        array[index1] = array[index2];
        array[index2] = tmp;
    }

}
