/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.container.Pair;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class BytesFunctionsTest {

    private static byte[] getBytes(final int length) {
        final byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) i;
        }
        return bytes;
    }

    /**
     * 文字列探索のテスト。
     */
    @Test
    public void testIndexOf() {
        final int length = 100;
        final byte[] bytes = getBytes(length);
        final int startIndex = 0;
        final int lastIndex = length;
        final List<Pair<Integer, byte[]>> list = new ArrayList<>();
        list.add(new Pair<>(45, new byte[] { 45, 46, 47 }));
        list.add(new Pair<>(-1, new byte[] { 45, 30, 12 }));
        list.add(new Pair<>(-1, new byte[] { length - 1, length }));
        list.add(new Pair<>(-1, new byte[] { length }));
        list.add(new Pair<>(length - 2, new byte[] { length - 2, length - 1 }));
        list.add(new Pair<>(length - 1, new byte[] { length - 1 }));
        list.add(new Pair<>(0, new byte[] { 0, 1 }));
        list.add(new Pair<>(0, new byte[] { 0 }));
        // 通常。
        for (final Pair<Integer, byte[]> pair : list) {
            Assert.assertEquals(pair.getFirst().intValue(), BytesFunctions.indexOf(bytes, pair.getSecond(), startIndex, lastIndex));
        }
    }

}
