package nippon.kawauso.chiraura.lib.math;

import java.math.BigInteger;


import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class MathFunctionsTest {

    /**
     * 最高位の1の位置を探せるか検査。
     */
    
    @Test
    public void testGetHighestSetBit() {
        Assert.assertEquals(13, MathFunctions.getHighestSetBit(BigInteger.valueOf(0x2c34L), 100));
        Assert.assertEquals(0, MathFunctions.getHighestSetBit(BigInteger.valueOf(0x1L), 100));
        Assert.assertEquals(9, MathFunctions.getHighestSetBit(BigInteger.valueOf(0x13a4L), 10));
        Assert.assertEquals(-1, MathFunctions.getHighestSetBit(BigInteger.valueOf(0x2c00L), 7));
    }

}
