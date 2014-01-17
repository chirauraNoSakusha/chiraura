/**
 * 
 */
package nippon.kawauso.chiraura.lib.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class BytesConversionTest {

    /**
     * @throws Exception 異常
     */
    @Test
    public void testSample() throws Exception {
        final long seed = System.nanoTime();
        final Random random = new Random(seed);
        final byte b = (byte) random.nextInt(Byte.MAX_VALUE - Byte.MIN_VALUE);
        final short s = (short) random.nextInt(Short.MAX_VALUE - Short.MIN_VALUE);
        final int i = random.nextInt();
        final long l = random.nextLong();

        final StringBuilder fmt = new StringBuilder(20);
        final Object[] inputs = new Object[14];
        final Object[] outputs = new Object[14];
        fmt.append('b');
        inputs[0] = b;
        outputs[0] = new byte[1];
        fmt.append('s');
        inputs[1] = s;
        outputs[1] = new short[1];
        fmt.append('i');
        inputs[2] = i;
        outputs[2] = new int[1];
        fmt.append('l');
        inputs[3] = l;
        outputs[3] = new long[1];
        fmt.append("cs");
        inputs[4] = s;
        outputs[4] = new short[1];
        fmt.append("ci");
        inputs[5] = i;
        outputs[5] = new int[1];
        fmt.append("cl");
        inputs[6] = l;
        outputs[6] = new long[1];
        fmt.append("ab");
        inputs[7] = new byte[] { b };
        outputs[7] = new byte[1][];
        fmt.append("as");
        inputs[8] = new short[] { s };
        outputs[8] = new short[1][];
        fmt.append("ai");
        inputs[9] = new int[] { i };
        outputs[9] = new int[1][];
        fmt.append("al");
        inputs[10] = new long[] { l };
        outputs[10] = new long[1][];
        fmt.append("acs");
        inputs[11] = new short[] { s };
        outputs[11] = new short[1][];
        fmt.append("aci");
        inputs[12] = new int[] { i };
        outputs[12] = new int[1][];
        fmt.append("acl");
        inputs[13] = new long[] { l };
        outputs[13] = new long[1][];

        final int size = BytesConversion.byteSize(new String(fmt), inputs);
        final int offset = random.nextInt(1000);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final int size1 = BytesConversion.toStream(out, new String(fmt), inputs);
        final byte[] buff = new byte[offset + size];
        System.arraycopy(out.toByteArray(), 0, buff, offset, size);
        Assert.assertEquals(size, size1);
        final int size2 = BytesConversion.fromStream(new ByteArrayInputStream(buff, offset, buff.length - offset), size, new String(fmt), outputs);
        Assert.assertEquals(size, size2);
        Assert.assertEquals(b, ((byte[]) outputs[0])[0]);
        Assert.assertEquals(s, ((short[]) outputs[1])[0]);
        Assert.assertEquals(i, ((int[]) outputs[2])[0]);
        Assert.assertEquals(l, ((long[]) outputs[3])[0]);
        Assert.assertEquals(s, ((short[]) outputs[4])[0]);
        Assert.assertEquals(i, ((int[]) outputs[5])[0]);
        Assert.assertEquals(l, ((long[]) outputs[6])[0]);
        Assert.assertArrayEquals(new byte[] { b }, ((byte[][]) outputs[7])[0]);
        Assert.assertArrayEquals(new short[] { s }, ((short[][]) outputs[8])[0]);
        Assert.assertArrayEquals(new int[] { i }, ((int[][]) outputs[9])[0]);
        Assert.assertArrayEquals(new long[] { l }, ((long[][]) outputs[10])[0]);
        Assert.assertArrayEquals(new short[] { s }, ((short[][]) outputs[11])[0]);
        Assert.assertArrayEquals(new int[] { i }, ((int[][]) outputs[12])[0]);
        Assert.assertArrayEquals(new long[] { l }, ((long[][]) outputs[13])[0]);
    }
}
