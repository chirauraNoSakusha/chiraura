/**
 * 
 */
package nippon.kawauso.chiraura.lib;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class ArrayFunctionsTest {

    private static interface Selector<T> {
        void select(T[] array, int n, int fromIndex, int toIndex);
    }

    private final Comparator<Integer> comparator = new Comparator<Integer>() {
        @Override
        public int compare(final Integer o1, final Integer o2) {
            return o1.compareTo(o2);
        }
    };

    private final Selector<Integer> naive = new Selector<Integer>() {
        @Override
        public void select(final Integer[] array, final int n, final int fromIndex, final int toIndex) {
            Arrays.sort(array, fromIndex, toIndex, ArrayFunctionsTest.this.comparator);
        }
    };

    private final Selector<Integer> quick = new Selector<Integer>() {
        @Override
        public void select(final Integer[] array, final int n, final int fromIndex, final int toIndex) {
            ArrayFunctions.orderSelect(array, n, fromIndex, toIndex, ArrayFunctionsTest.this.comparator);
        }
    };

    private final Random random = new Random();

    private final Selector<Integer> randomSelector = new Selector<Integer>() {
        @Override
        public void select(final Integer[] array, final int n, final int fromIndex, final int toIndex) {
            ArrayFunctions.randomSelect(array, n, fromIndex, toIndex);
        }
    };

    private static <T> void testInvariantUnit(final T[] array, final int n, final int fromIndex, final int toIndex, final Selector<T> selector,
            final Comparator<T> comparator) {
        // 選択しても配列の構成は変わらないことの検査。

        final T[] copy = Arrays.copyOf(array, array.length);
        selector.select(copy, n, fromIndex, toIndex);

        // 候補範囲。
        final T[] candidates1 = Arrays.copyOfRange(array, fromIndex, toIndex);
        final T[] candidates2 = Arrays.copyOfRange(copy, fromIndex, toIndex);
        Arrays.sort(candidates1, comparator);
        Arrays.sort(candidates2, comparator);
        Assert.assertArrayEquals(candidates1, candidates2);
        // 前。
        Assert.assertArrayEquals(Arrays.copyOf(array, fromIndex), Arrays.copyOf(copy, fromIndex));
        // 後。
        Assert.assertArrayEquals(Arrays.copyOfRange(array, toIndex, array.length), Arrays.copyOfRange(copy, toIndex, copy.length));
    }

    /**
     * クイックソートみたいな実装の試験。
     */
    @Test
    public void testQuickInvariant() {
        final int loop = 10_000;
        final int length = 1_000;
        for (int i = 0; i < loop; i++) {
            final Integer[] array = new Integer[this.random.nextInt(length)];
            for (int j = 0; j < array.length; j++) {
                array[j] = this.random.nextInt();
            }
            final int n = array.length == 0 ? 0 : this.random.nextInt(array.length);
            final int fromIndex = array.length == 0 ? 0 : this.random.nextInt(array.length);
            final int toIndex = fromIndex + (array.length - fromIndex == 0 ? 0 : this.random.nextInt(array.length - fromIndex));
            testInvariantUnit(array, n, fromIndex, toIndex, this.quick, this.comparator);
        }
    }

    /**
     * クイックソートみたいな実装の試験。
     */
    @Test
    public void testRandomInvariant() {
        final int loop = 10_000;
        final int length = 1_000;
        for (int i = 0; i < loop; i++) {
            final Integer[] array = new Integer[this.random.nextInt(length)];
            for (int j = 0; j < array.length; j++) {
                array[j] = this.random.nextInt();
            }
            final int n = array.length == 0 ? 0 : this.random.nextInt(array.length);
            final int fromIndex = array.length == 0 ? 0 : this.random.nextInt(array.length);
            final int toIndex = fromIndex + (array.length - fromIndex == 0 ? 0 : this.random.nextInt(array.length - fromIndex));
            testInvariantUnit(array, n, fromIndex, toIndex, this.randomSelector, this.comparator);
        }
    }

    private static <T> void testQuickUnit(final T[] array, final int n, final int fromIndex, final int toIndex, final Selector<T> selector1,
            final Selector<T> selector2, final Comparator<T> comparator) {
        final T[] copy = Arrays.copyOf(array, array.length);
        selector1.select(array, n, fromIndex, toIndex);
        selector2.select(copy, n, fromIndex, toIndex);

        // 選択箇所。
        final T[] selected1 = Arrays.copyOfRange(array, fromIndex, Math.min(fromIndex + n, toIndex));
        final T[] selected2 = Arrays.copyOfRange(copy, fromIndex, Math.min(fromIndex + n, toIndex));
        Arrays.sort(selected1, comparator);
        Arrays.sort(selected2, comparator);
        Assert.assertArrayEquals(selected1, selected2);
        // 残りの候補。
        final T[] notSelected1 = Arrays.copyOfRange(array, Math.min(fromIndex + n, toIndex), toIndex);
        final T[] notSelected2 = Arrays.copyOfRange(copy, Math.min(fromIndex + n, toIndex), toIndex);
        Arrays.sort(notSelected1, comparator);
        Arrays.sort(notSelected2, comparator);
        Assert.assertArrayEquals(notSelected1, notSelected2);
        // 前。
        Assert.assertArrayEquals(Arrays.copyOf(array, fromIndex), Arrays.copyOf(copy, fromIndex));
        // 前。
        Assert.assertArrayEquals(Arrays.copyOfRange(array, toIndex, array.length), Arrays.copyOfRange(copy, toIndex, copy.length));
    }

    /**
     * クイックソートみたいな実装の試験。
     */
    @Test
    public void testQuick() {
        final int loop = 10_000;
        final int length = 1_000;
        for (int i = 0; i < loop; i++) {
            final Integer[] array = new Integer[this.random.nextInt(length)];
            for (int j = 0; j < array.length; j++) {
                array[j] = this.random.nextInt();
            }
            final int n = array.length == 0 ? 0 : this.random.nextInt(array.length);
            final int fromIndex = array.length == 0 ? 0 : this.random.nextInt(array.length);
            final int toIndex = fromIndex + (array.length - fromIndex == 0 ? 0 : this.random.nextInt(array.length - fromIndex));
            testQuickUnit(array, n, fromIndex, toIndex, this.quick, this.naive, this.comparator);
        }
    }

    /**
     * クイックソートみたいな実装の性能試験。
     */
    // @Test
    public void testQuickPerformance() {
        final int loop = 10_000;
        final int length = 1_000;
        final Integer[][] arrays1 = new Integer[loop][];
        final Integer[][] arrays2 = new Integer[loop][];
        final int[] ns = new int[loop];
        final int[] fronIndices = new int[loop];
        final int[] toIndices = new int[loop];
        for (int i = 0; i < loop; i++) {
            final Integer[] array = new Integer[this.random.nextInt(length)];
            for (int j = 0; j < array.length; j++) {
                array[j] = this.random.nextInt();
            }
            final int n = array.length == 0 ? 0 : this.random.nextInt(array.length);
            final int fromIndex = array.length == 0 ? 0 : this.random.nextInt(array.length);
            final int toIndex = fromIndex + (array.length - fromIndex == 0 ? 0 : this.random.nextInt(array.length - fromIndex));

            arrays1[i] = array.clone();
            arrays2[i] = array.clone();
            ns[i] = n;
            fronIndices[i] = fromIndex;
            toIndices[i] = toIndex;
        }

        final long start1 = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            this.naive.select(arrays1[i], ns[i], fronIndices[i], toIndices[i]);
        }
        final long end1 = System.nanoTime();
        System.out.println("naive " + ((end1 - start1) / 1_000_000_000.0));
        final long start2 = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            this.quick.select(arrays2[i], ns[i], fronIndices[i], toIndices[i]);
        }
        final long end2 = System.nanoTime();
        System.out.println("quick " + ((end2 - start2) / 1_000_000_000.0));
    }

}
