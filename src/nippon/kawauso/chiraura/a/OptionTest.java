package nippon.kawauso.chiraura.a;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import nippon.kawauso.chiraura.Global;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class OptionTest {

    /**
     * 設定ファイルが読めるかどうかの検査。
     * @throws Exception 異常
     */
    @Test
    public void testLoad() throws Exception {
        final File file = File.createTempFile("chiraura", null);
        try (BufferedWriter buff = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Global.INTERNAL_CHARSET))) {
            buff.write(Option.Item.port + " = 100");
            buff.newLine();
        }
        final Option option = new Option(file.getPath());
        Assert.assertEquals("100", option.get(Option.Item.port));
    }

}
