/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib;

/**
 * @author chirauraNoSakusha
 */
public final class BytesFunctions {

    // インスタンス化防止。
    private BytesFunctions() {}

    /**
     * バイト列の中で与えられたバイト列に等しい最初の箇所の先頭を求める。
     * @param bytes 入力バイト列
     * @param pattern 探すバイト列
     * @param firstIndex 比較に使う先頭位置
     * @param lastIndex 比較に使わない先頭位置
     * @return targetに等しい最初の箇所の先頭位置。無い場合は-1
     */
    public static int indexOf(final byte[] bytes, final byte[] pattern, final int firstIndex, final int lastIndex) {
        for (int i = firstIndex; i <= lastIndex - pattern.length; i++) {
            if (bytes[i] == pattern[0]) {
                boolean isEqual = true;
                for (int j = 1; j < pattern.length; j++) {
                    if (bytes[i + j] != pattern[j]) {
                        isEqual = false;
                        break;
                    }
                }
                if (isEqual) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * バイト列の比較。
     * 前方から比較し、異なる値があれば、結果を返す。
     * 長さが共通する部分に異なる値が無ければ、短い方が小さいとする。
     * @param b1 比較対象
     * @param b2 比較対象
     * @return b1がb2より小さければ負値。大きければ正値。等しければ0
     */
    public static int compare(final byte[] b1, final byte[] b2) {
        for (int i = 0, n = Math.min(b1.length, b2.length); i < n; i++) {
            if (b1[i] < b2[i]) {
                return -1;
            } else if (b1[i] > b2[i]) {
                return 1;
            }
        }
        return b1.length - b2.length;
    }

}
