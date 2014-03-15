package nippon.kawauso.chiraura.lib;

import java.util.ArrayList;

/**
 * @author chirauraNoSakusha
 */
public final class ArrayListFunctions {

    // インスタンス化防止。
    private ArrayListFunctions() {}

    /**
     * 配列の要素を伸ばす方向にずらす。
     * ずらした分、配列は伸びる。
     * ずらして空いた前部には null が入る。
     * @param <T> 配列要素のクラス
     * @param array 配列
     * @param shiftLength ずらす数
     */
    public static <T> void shiftRight(final ArrayList<T> array, final int shiftLength) {
        final int beforeSize = array.size();
        if (beforeSize < shiftLength) {
            /*
             * |1|2|3|4|5|
             * を
             * |1|2|3|4|5|n|n|n|
             * にする。
             */
            for (int i = 0; i < shiftLength - beforeSize; i++) {
                array.add(null);
            }
            /*
             * |1|2|3|4|5|n|n|n|
             * を
             * |n|n|n|n|n|n|n|n|1|2|3|4|5|
             * にする。
             */
            for (int i = 0; i < beforeSize; i++) {
                array.add(array.get(i));
                array.set(i, null);
            }
        } else {
            /*
             * |1|2|3|4|5|
             * を
             * |1|2|3|4|5|3|4|5|
             * にする。
             */
            for (int i = beforeSize - shiftLength; i < beforeSize; i++) {
                array.add(array.get(i));
            }
            /*
             * |1|2|3|4|5|3|4|5|
             * を
             * |1|2|3|1|2|3|4|5|
             * にする。
             */
            for (int i = beforeSize - shiftLength - 1; i >= 0; i--) {
                array.set(i + shiftLength, array.get(i));
            }
            /*
             * |1|2|3|1|2|3|4|5|
             * を
             * |n|n|n|1|2|3|4|5|
             * にする。
             */
            for (int i = 0; i < shiftLength; i++) {
                array.set(i, null);
            }
        }
    }

    /**
     * 配列の要素を縮める方向にずらす。
     * ずらした分、配列は縮む。
     * @param <T> 配列要素にクラス
     * @param array 配列
     * @param shiftLength ずらす数
     */
    public static <T> void shiftLeft(final ArrayList<T> array, final int shiftLength) {
        final int beforeSize = array.size();
        /*
         * |1|2|3|4|5|6|7|
         * を
         * |4|5|6|7|5|6|7|
         * にする。
         */
        for (int i = shiftLength; i < beforeSize; i++) {
            array.set(i - shiftLength, array.get(i));
        }
        /*
         * |4|5|6|7|5|6|7|
         * を
         * |4|5|6|7|
         * にする。
         */
        for (int i = beforeSize - 1; i >= Math.max(0, beforeSize - shiftLength); i--) {
            array.remove(i);
        }
    }

}
