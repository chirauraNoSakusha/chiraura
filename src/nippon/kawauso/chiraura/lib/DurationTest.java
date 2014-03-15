package nippon.kawauso.chiraura.lib;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public class DurationTest {

    /**
     * 検査。
     */
    @Test
    public void testToString() {
        Assert.assertEquals("0ミリ秒", Duration.toString(0L));
        Assert.assertEquals("1ミリ秒", Duration.toString(1L));
        Assert.assertEquals("999ミリ秒", Duration.toString(999L));
        Assert.assertEquals("1秒", Duration.toString(Duration.SECOND));
        Assert.assertEquals("1秒くらい", Duration.toString(Duration.SECOND + 1));

        Assert.assertEquals("9秒くらい", Duration.toString(10 * Duration.SECOND - 501));
        Assert.assertEquals("10秒くらい", Duration.toString(10 * Duration.SECOND - 500));
        Assert.assertEquals("10秒くらい", Duration.toString(10 * Duration.SECOND - 1));
        Assert.assertEquals("10秒", Duration.toString(10 * Duration.SECOND));
        Assert.assertEquals("10秒くらい", Duration.toString(10 * Duration.SECOND + 1));
        Assert.assertEquals("10秒くらい", Duration.toString(10 * Duration.SECOND + 499));
        Assert.assertEquals("11秒くらい", Duration.toString(10 * Duration.SECOND + 500));
    }
}
