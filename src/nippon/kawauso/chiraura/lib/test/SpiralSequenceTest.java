package nippon.kawauso.chiraura.lib.test;

import org.junit.Assert;

import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class SpiralSequenceTest {

    /**
     * 最初と最後のあたりの検査。
     */

    @Test
    public void testBounds() {
        SpiralSequence sequence;

        sequence = new SpiralSequence(0);
        Assert.assertEquals(0, sequence.nextLong());
        Assert.assertEquals(-1, sequence.nextLong());
        Assert.assertEquals(1, sequence.nextLong());

        sequence = new SpiralSequence(Long.MAX_VALUE);
        Assert.assertEquals(Long.MAX_VALUE, sequence.nextLong());
        Assert.assertEquals(Long.MIN_VALUE, sequence.nextLong());
        Assert.assertEquals(0, sequence.nextLong());

        sequence = new SpiralSequence(Integer.MAX_VALUE);
        Assert.assertEquals(Integer.MAX_VALUE, sequence.nextInt());
        Assert.assertEquals(Integer.MIN_VALUE, sequence.nextInt());
        Assert.assertEquals(0, sequence.nextInt());

        sequence = new SpiralSequence(Short.MAX_VALUE);
        Assert.assertEquals(Short.MAX_VALUE, sequence.nextShort());
        Assert.assertEquals(Short.MIN_VALUE, sequence.nextShort());
        Assert.assertEquals(0, sequence.nextShort());

        sequence = new SpiralSequence(Byte.MAX_VALUE);
        Assert.assertEquals(Byte.MAX_VALUE, sequence.nextByte());
        Assert.assertEquals(Byte.MIN_VALUE, sequence.nextByte());
        Assert.assertEquals(0, sequence.nextByte());
    }

}
