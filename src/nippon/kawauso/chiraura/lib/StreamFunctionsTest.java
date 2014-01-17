/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nippon.kawauso.chiraura.lib;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class StreamFunctionsTest {

    /**
     * スキップ中に入力が尽きたらエラーが帰ることを検査。
     * @throws MyRuleException 規約違反
     * @throws IOException エラー
     */
    @Test(expected = MyRuleException.class)
    public void testCompleteSkipEof() throws MyRuleException, IOException {
        final byte[] buff = new byte[100];
        try (final InputStream rawInput = new ByteArrayInputStream(buff);
                final InputStream input = new BufferedInputStream(rawInput)) {
            StreamFunctions.completeSkip(input, 150);
        }
    }

    /**
     * @throws MyRuleException 規約違反
     * @throws IOException エラー
     */
    @Test
    public void testCompleteSkip() throws MyRuleException, IOException {
        final int[] lengths = new int[20];
        final long seed = System.nanoTime();
        final Random random = new Random(seed);
        final int maxLength = 500;
        int sum = 0;
        for (int i = 0; i < lengths.length; i++) {
            lengths[i] = random.nextInt(maxLength);
            sum += lengths[i];
        }
        final byte[] buff = new byte[sum];
        try (final ByteArrayInputStream rawInput = new ByteArrayInputStream(buff);
                final InputStream input = new BufferedInputStream(rawInput)) {
            for (final int length : lengths) {
                StreamFunctions.completeSkip(input, length);
            }
            Assert.assertTrue(StreamFunctions.isEof(input));
        }
    }

}
