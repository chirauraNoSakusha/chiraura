/**
 * 
 */
package nippon.kawauso.chiraura.lib.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.test.RandomString;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class InputStreamWrapperTest {

    /**
     * 検査。
     * @throws Exception 異常
     */
    @Test
    public void testSample() throws Exception {
        final Random random = new Random();
        final int loop = 100_000;
        final int length = 1_000;
        final String separator = "\r\n";

        final List<String> samples = new ArrayList<>();
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int i = 0; i < loop; i++) {
            final String sample = RandomString.nextString(length / 3, random);
            samples.add(sample);
            output.write(sample.getBytes());
            output.write(separator.getBytes());
        }

        try (final InputStreamWrapper input = new InputStreamWrapper(new ByteArrayInputStream(output.toByteArray()), Charset.defaultCharset(), separator,
                length)) {
            for (int i = 0; i < samples.size(); i++) {
                // System.out.println("Aho " + i);
                final String line = input.readLine();
                Assert.assertEquals(samples.get(i), line);
            }

            Assert.assertNull(input.readLine());
        }
    }

    /**
     * mark, reset の検査。
     * @throws Exception 異常
     */
    @Test
    public void testMark() throws Exception {
        final int loop = 10_000;
        final int length = 1000;
        final Charset charset = Charset.defaultCharset();
        final String separator = "\r\n";

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (int i = 0; i < loop; i++) {
            // 文字の区切りで分けられるとうまくいかないので ASCII
            final String sample = RandomString.nextAsciiString(length / 3, ThreadLocalRandom.current());
            output.write(sample.getBytes(charset));
            output.write(separator.getBytes(charset));
        }

        final InputStream rival = new ByteArrayInputStream(output.toByteArray());
        final InputStreamWrapper instance = new InputStreamWrapper(new ByteArrayInputStream(output.toByteArray()), charset, separator, length);

        final byte[] buff1 = new byte[length];
        final byte[] buff2 = new byte[length];

        boolean marked = false;
        int len = 0;
        while (true) {
            if (marked) {
                if (len == length || ThreadLocalRandom.current().nextInt(4) == 0) {
                    rival.reset();
                    instance.reset();
                    marked = false;
                } else {
                    final int size = ThreadLocalRandom.current().nextInt(length - len);
                    final int len1 = rival.read(buff1, 0, size);
                    final int len2 = instance.read(buff2, 0, size);
                    if (len1 < 0) {
                        Assert.assertEquals(len1, len2);
                        break;
                    }
                    Assert.assertArrayEquals(buff1, buff2);
                    len += size;
                }
            } else {
                if (ThreadLocalRandom.current().nextInt(4) == 0) {
                    rival.mark(length);
                    instance.mark(length);
                    marked = true;
                    len = 0;
                } else if (ThreadLocalRandom.current().nextBoolean()) {
                    final int size = ThreadLocalRandom.current().nextInt(length);
                    final int len1 = rival.read(buff1, 0, size);
                    final int len2 = instance.read(buff2, 0, size);
                    if (len1 < 0) {
                        Assert.assertEquals(len1, len2);
                        break;
                    }
                    Assert.assertArrayEquals(buff1, buff2);
                } else {
                    final String line = instance.readLine();
                    if (line == null) {
                        Assert.assertEquals(-1, rival.read());
                        break;
                    } else {
                        final byte[] buff3 = line.getBytes(charset);
                        final byte[] buff4 = StreamFunctions.completeRead(rival, buff3.length);
                        Assert.assertEquals(new String(buff4, charset), line);
                        rival.skip(separator.getBytes(charset).length);
                    }
                }
            }
        }

        instance.close();
    }

    /**
     * しつこい検査。
     * @throws Exception 異常
     */
    // @Test
    public void testMarkLoop() throws Exception {
        while (true) {
            testMark();
        }
    }
}
