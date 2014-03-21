package nippon.kawauso.chiraura.lib.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class FileFunctionsTest {

    /**
     * ちゃんと消せることの検査。
     * @throws Exception 異常
     */
    @Test
    public void testDeleteContents() throws Exception {
        final Random random = new Random();
        final File root = new File(System.getProperty("java.io.tmpdir") + File.separator + this.getClass().getName() + File.separator + System.nanoTime());
        root.mkdirs();
        Assert.assertTrue(root.exists());

        final Queue<File> directories = new LinkedList<>();
        directories.add(root);
        for (int i = 0; i < 100; i++) {
            final File directory = directories.poll();
            if (directory == null) {
                break;
            }

            for (int j = 0; j < 10; j++) {
                final File file = new File(directory, Integer.toString(j));
                if (random.nextInt(2) == 0) {
                    file.mkdirs();
                    Assert.assertTrue(file.exists());
                    directories.add(file);
                } else {
                    Assert.assertTrue(file.createNewFile());
                    try (OutputStream output = new FileOutputStream(file)) {
                        output.write(i);
                    }
                }
            }
        }

        Assert.assertTrue(FileFunctions.deleteContents(root));
        Assert.assertTrue(root.delete());
    }

    /**
     * 同じものを同じと言う検査。
     * @throws Exception 異常
     */
    @Test
    public void equalsTest() throws Exception {
        final Random random = new Random();
        final long date = System.nanoTime();
        final File root1 = new File(System.getProperty("java.io.tmpdir") + File.separator + this.getClass().getName() + File.separator + date);
        final File root2 = new File(System.getProperty("java.io.tmpdir") + File.separator + this.getClass().getName() + File.separator + (date + 1));
        Assert.assertTrue(root1.mkdirs());
        Assert.assertTrue(root2.mkdirs());

        final Queue<File> directories1 = new LinkedList<>();
        final Queue<File> directories2 = new LinkedList<>();
        directories1.add(root1);
        directories2.add(root2);
        for (int i = 0; i < 100; i++) {
            final File directory1 = directories1.poll();
            final File directory2 = directories2.poll();
            if (directory1 == null) {
                break;
            }

            for (int j = 0; j < 10; j++) {
                final File file1 = new File(directory1, Integer.toString(j));
                final File file2 = new File(directory2, Integer.toString(j));
                if (random.nextInt(2) == 0) {
                    Assert.assertTrue(file1.mkdir());
                    Assert.assertTrue(file2.mkdir());
                    directories1.add(file1);
                    directories2.add(file2);
                } else {
                    Assert.assertTrue(file1.createNewFile());
                    Assert.assertTrue(file2.createNewFile());
                    try (OutputStream output1 = new FileOutputStream(file1);
                            OutputStream output2 = new FileOutputStream(file2)) {
                        final byte[] buff = new byte[10_000];
                        random.nextBytes(buff);
                        output1.write(buff);
                        output2.write(buff);
                    }
                }
            }
        }

        Assert.assertTrue(FileFunctions.directoryEquals(root1, root2));
        Assert.assertTrue(FileFunctions.directoryEquals(root2, root1));
        Assert.assertTrue(FileFunctions.deleteContents(root1));
        Assert.assertTrue(FileFunctions.deleteContents(root2));
        Assert.assertTrue(root1.delete());
        Assert.assertTrue(root2.delete());
    }

    /**
     * 違うなら違うと言えるかの検査。
     * @throws Exception 異常
     */
    @Test
    public void notEqualsTest() throws Exception {
        final long date = System.nanoTime();
        final File root1 = new File(System.getProperty("java.io.tmpdir") + File.separator + this.getClass().getName() + File.separator + date);
        final File root2 = new File(System.getProperty("java.io.tmpdir") + File.separator + this.getClass().getName() + File.separator + (date + 1));
        Assert.assertTrue(root1.mkdirs());
        Assert.assertTrue(root2.mkdirs());

        Assert.assertTrue(FileFunctions.directoryEquals(root1, root2));
        Assert.assertTrue(FileFunctions.directoryEquals(root2, root1));

        // 余分なディレクトリ。
        Assert.assertTrue((new File(root1, "file1")).mkdir());
        Assert.assertFalse(FileFunctions.directoryEquals(root1, root2));
        Assert.assertFalse(FileFunctions.directoryEquals(root2, root1));

        // 同じに直す。
        Assert.assertTrue((new File(root2, "file1")).mkdir());
        Assert.assertTrue(FileFunctions.directoryEquals(root1, root2));
        Assert.assertTrue(FileFunctions.directoryEquals(root2, root1));

        // 余分なファイル。
        Assert.assertTrue((new File(root1, "file2")).createNewFile());
        Assert.assertFalse(FileFunctions.directoryEquals(root1, root2));
        Assert.assertFalse(FileFunctions.directoryEquals(root2, root1));

        // 同じに直す。
        Assert.assertTrue((new File(root2, "file2")).createNewFile());
        Assert.assertTrue(FileFunctions.directoryEquals(root1, root2));
        Assert.assertTrue(FileFunctions.directoryEquals(root2, root1));

        // 違うファイル。
        final File file1 = new File(root1, "file3");
        final File file2 = new File(root2, "file3");
        Assert.assertTrue(file1.createNewFile());
        Assert.assertTrue(file2.createNewFile());
        try (OutputStream output1 = new FileOutputStream(file1);
                OutputStream output2 = new FileOutputStream(file2)) {
            output1.write(0);
            output2.write(1);
        }
        Assert.assertFalse(FileFunctions.directoryEquals(root1, root2));
        Assert.assertFalse(FileFunctions.directoryEquals(root2, root1));

        // 同じに直す。
        try (OutputStream output2 = new FileOutputStream(file2)) {
            output2.write(0);
        }
        Assert.assertTrue(FileFunctions.directoryEquals(root1, root2));
        Assert.assertTrue(FileFunctions.directoryEquals(root2, root1));

        // ディレクトリとファイル。
        Assert.assertTrue((new File(root1, "file4")).mkdir());
        Assert.assertTrue((new File(root2, "file4")).createNewFile());
        Assert.assertFalse(FileFunctions.directoryEquals(root1, root2));
        Assert.assertFalse(FileFunctions.directoryEquals(root2, root1));

        // 同じに直す。
        Assert.assertTrue((new File(root2, "file4")).delete());
        Assert.assertTrue((new File(root2, "file4")).mkdir());
        Assert.assertTrue(FileFunctions.directoryEquals(root1, root2));
        Assert.assertTrue(FileFunctions.directoryEquals(root2, root1));

        Assert.assertTrue(FileFunctions.deleteContents(root1));
        Assert.assertTrue(FileFunctions.deleteContents(root2));
        Assert.assertTrue(root1.delete());
        Assert.assertTrue(root2.delete());
    }

}
