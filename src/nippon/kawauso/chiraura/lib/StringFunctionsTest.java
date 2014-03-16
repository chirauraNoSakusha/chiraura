package nippon.kawauso.chiraura.lib;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.test.RandomString;
import nippon.kawauso.chiraura.lib.test.TestFunctions;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class StringFunctionsTest {

    private static final Logger LOG = Logger.getLogger(StringFunctionsTest.class.getName());

    /**
     * 初期化
     */
    public StringFunctionsTest() {
        TestFunctions.testLogging(this.getClass().getSimpleName());
    }

    /**
     * 文字列幅の検査。
     */
    @Test
    public void testGetWidth() {
        Assert.assertEquals(5, StringFunctions.getWidth("あsか"));
        Assert.assertEquals(25, StringFunctions.getWidth("んrtsう゛ぃるracんiづonい"));
    }

    /**
     * @throws MyRuleException 変換規約違反
     */
    @Test
    public void testUrlDecode() throws MyRuleException {
        Assert.assertEquals("てすとスレ", StringFunctions.urlDecode("%82%C4%82%B7%82%C6%83X%83%8C", Charset.forName("Shift_JIS")));
        Assert.assertEquals("名無し", StringFunctions.urlDecode("%96%BC%96%B3%82%B5", Charset.forName("Shift_JIS")));
        Assert.assertEquals("新規スレッド作成", StringFunctions.urlDecode("%90V%8BK%83X%83%8C%83b%83h%8D%EC%90%AC", Charset.forName("Shift_JIS")));
        Assert.assertEquals("てすと", StringFunctions.urlDecode("%82%C4%82%B7%82%C6", Charset.forName("Shift_JIS")));
        Assert.assertEquals("書き込む", StringFunctions.urlDecode("%8F%91%82%AB%8D%9E%82%DE", Charset.forName("Shift_JIS")));
    }

    /**
     * @throws Exception 異常
     */
    @Test
    public void testUrlEncoderPerformance() throws Exception {
        final int loop = 100_000;
        final int length = 100;
        final Charset charset = Charset.forName("UTF-8");

        final Random random = new Random();
        final long start = System.nanoTime();
        for (int i = 0; i < loop; i++) {
            final String string = RandomString.nextString(length, random);
            final String decoded = StringFunctions.urlDecode(StringFunctions.urlEncode(string, charset), charset);
            Assert.assertEquals(string, decoded);
        }
        final long end = System.nanoTime();
        LOG.log(Level.SEVERE, "繰り返し回数: {0}, 文字列長: {1}, 1文字列当たりの消費ミリ秒: {2}", new Object[] { loop, length, (end - start) / (1_000_000.0 * loop) });
    }
}
