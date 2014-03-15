package nippon.kawauso.chiraura.lib;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class ArrayListFunctionsTest {

    private static final String[] TABLE = new String[] { "い", "ろ", "は", "に", "ほ", "へ", "と", };
    ArrayList<String> array = null;

    /**
     * 初期化。
     */
    public ArrayListFunctionsTest() {
        this.array = new ArrayList<>();
        for (final String value : TABLE) {
            this.array.add(value);
        }
    }

    private void testShiftRight(final int shiftLength) {
        ArrayListFunctions.shiftRight(this.array, shiftLength);
        // System.out.println(this.array);
        Assert.assertEquals(TABLE.length + shiftLength, this.array.size());
        for (int i = 0; i < shiftLength; i++) {
            Assert.assertNull(this.array.get(i));
        }
        for (int i = 0; i < TABLE.length; i++) {
            Assert.assertEquals(TABLE[i], this.array.get(shiftLength + i));
        }
    }

    /**
     * 伸びる方向への検査 (移動量0)。
     */
    @Test
    public void testShiftRight0() {
        testShiftRight(0);
    }

    /**
     * 伸びる方向への検査 (移動量1)。
     */
    @Test
    public void testShiftRight1() {
        testShiftRight(1);
    }

    /**
     * 伸びる方向への検査 (移動量小)。
     */
    @Test
    public void testShiftRightSmall() {
        testShiftRight(TABLE.length / 2);
    }

    /**
     * 伸びる方向への検査 (移動量大)。
     */
    @Test
    public void testShiftRightBig() {
        testShiftRight(TABLE.length * 2);
    }

    private void testShiftLeft(final int shiftLength) {
        ArrayListFunctions.shiftLeft(this.array, shiftLength);
        // System.out.println(this.array);
        Assert.assertEquals(Math.max(0, TABLE.length - shiftLength), this.array.size());
        for (int i = shiftLength; i < TABLE.length; i++) {
            Assert.assertEquals(TABLE[i], this.array.get(i - shiftLength));
        }
    }

    /**
     * 縮む方向への検査 (移動量0)。
     */
    @Test
    public void testShiftLeft0() {
        testShiftLeft(0);
    }

    /**
     * 縮む方向への検査 (移動量1)。
     */
    @Test
    public void testShiftLeft1() {
        testShiftLeft(1);
    }

    /**
     * 縮む方向への検査 (移動量小)。
     */
    @Test
    public void testShiftLeftSmall() {
        testShiftLeft(TABLE.length / 2);
    }

    /**
     * 縮む方向への検査 (移動量大)。
     */
    @Test
    public void testShiftLeftBig() {
        testShiftLeft(TABLE.length * 2);
    }

}
