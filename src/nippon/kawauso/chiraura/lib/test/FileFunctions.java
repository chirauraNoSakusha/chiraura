/**
 * 
 */
package nippon.kawauso.chiraura.lib.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
public final class FileFunctions {

    private static final Logger LOG = Logger.getLogger(FileFunctions.class.getName());

    // インスタンス化防止。
    private FileFunctions() {}

    /**
     * ディレクトリの中身を空にする。
     * @param directory 対象のディレクトリ
     * @return 空にできた場合のみ true
     */
    public static boolean deleteContents(final File directory) {
        for (final File file : directory.listFiles()) {
            if (file.isDirectory()) {
                deleteContents(file);
            }
            if (!file.delete()) {
                return false;
            }
        }
        return true;
    }

    private static final int READ_BUFFER_SIZE = 8192;

    private static boolean fileEquals(final File file1, final File file2) throws FileNotFoundException, IOException {
        final long size = file1.length();
        if (size != file2.length()) {
            return false;
        }

        try (InputStream input1 = new FileInputStream(file1);
                InputStream input2 = new FileInputStream(file2)) {
            if (size < READ_BUFFER_SIZE) {
                return Arrays.equals(StreamFunctions.completeRead(input1, (int) size), StreamFunctions.completeRead(input2, (int) size));
            } else {
                final byte[] buff1 = new byte[READ_BUFFER_SIZE];
                final byte[] buff2 = new byte[READ_BUFFER_SIZE];
                for (int readSize = 0; readSize < size; readSize += READ_BUFFER_SIZE) {
                    if (readSize + READ_BUFFER_SIZE <= size) {
                        StreamFunctions.completeRead(input1, buff1, 0, READ_BUFFER_SIZE);
                        StreamFunctions.completeRead(input2, buff2, 0, READ_BUFFER_SIZE);
                        if (!Arrays.equals(buff1, buff2)) {
                            return false;
                        }
                    } else {
                        final int remainSize = (int) (size - readSize);
                        return Arrays.equals(StreamFunctions.completeRead(input1, remainSize), StreamFunctions.completeRead(input2, remainSize));
                    }
                }
                // size % READ_BUFFER_SIZE == 0 だったとき。
                return true;
            }
        } catch (final MyRuleException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ディレクトリの中身が等しいかどうか検査
     * @param directory1 1 つ目の比較対象
     * @param directory2 2 つ目の比較対象
     * @return 等しい場合のみ true
     * @throws IOException 読み込み異常
     */
    public static boolean directoryEquals(final File directory1, final File directory2) throws IOException {
        final NavigableSet<File> files1 = new TreeSet<>(Arrays.asList(directory1.listFiles()));
        final NavigableSet<File> files2 = new TreeSet<>(Arrays.asList(directory2.listFiles()));
        // System.out.println(files1);
        // System.out.println(files2);
        final Iterator<File> iterator1 = files1.iterator();
        final Iterator<File> iterator2 = files2.iterator();
        while (iterator1.hasNext() && iterator2.hasNext()) {
            final File file1 = iterator1.next();
            final File file2 = iterator2.next();

            // System.out.println(file1 + " " + file2);
            if (!file1.getName().equals(file2.getName())) {
                if (!(new File(file1.getParentFile(), file2.getName())).exists()) {
                    LOG.log(Level.FINEST, (new File(file1.getParentFile(), file2.getName())).getPath() + " が無い。");
                } else {
                    LOG.log(Level.FINEST, (new File(file2.getParentFile(), file1.getName())).getPath() + " が無い。");
                }
                return false;
            } else if (file1.isDirectory()) {
                if (file2.isDirectory()) {
                    if (!directoryEquals(file1, file2)) {
                        return false;
                    }
                } else {
                    LOG.log(Level.FINEST, file1.getPath() + " はディレクトリなのに " + file2.getPath() + " はディレクトリじゃない。");
                    return false;
                }
            } else {
                if (file2.isDirectory()) {
                    LOG.log(Level.FINEST, file1.getPath() + " はディレクトリじゃないのに " + file2.getPath() + " はディレクトリ。");
                    return false;
                } else {
                    if (!fileEquals(file1, file2)) {
                        LOG.log(Level.FINEST, file1.getPath() + " と " + file2.getPath() + " が違う。");
                        return false;
                    }
                }
            }
        }

        if (iterator1.hasNext()) {
            LOG.log(Level.FINEST, (new File(directory2, iterator1.next().getName()).getPath() + " が無い。"));
            return false;
        } else if (iterator2.hasNext()) {
            LOG.log(Level.FINEST, (new File(directory1, iterator2.next().getName()).getPath() + " が無い。"));
            return false;
        }

        return true;
    }

}
