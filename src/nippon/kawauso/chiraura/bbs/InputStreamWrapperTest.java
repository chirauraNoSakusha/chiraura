/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
        }
    }

}
