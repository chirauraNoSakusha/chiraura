/**
 * 
 */
package nippon.kawauso.chiraura.lib.converter;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public class NumberBytesConversionTest {

    /**
     * 検査。
     */
    @Test
    public void testBytes() {
        Assert.assertArrayEquals(new byte[] { 0 }, NumberBytesConversion.toBytes(0));
        Assert.assertArrayEquals(new byte[] { 5 }, NumberBytesConversion.toBytes(5));
        Assert.assertArrayEquals(new byte[] { 40 }, NumberBytesConversion.toBytes(40));
        Assert.assertArrayEquals(new byte[] { 63 }, NumberBytesConversion.toBytes(63));
    }
}
