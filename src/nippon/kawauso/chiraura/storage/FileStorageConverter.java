package nippon.kawauso.chiraura.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.bbs.OrderingBoardChunk;
import nippon.kawauso.chiraura.bbs.SimpleBoardChunk;
import nippon.kawauso.chiraura.bbs.ThreadChunk;
import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.converter.Base32;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
public final class FileStorageConverter {

    private static final Logger LOG = Logger.getLogger(FileStorageConverter.class.getName());

    // インスタンス化防止。
    private FileStorageConverter() {}

    /**
     * FileStorage64 を FileStorage32 に変換する。
     * 非並列動作を想定。
     * @param root ルートディレクトリ
     * @param fileSizeLimit 最大ファイルサイズ
     * @param directoryBitSize ディレクトリに使うビット数
     * @throws IOException 読み書き異常
     * @throws InterruptedException 割り込まれた場合
     */
    public static void convert(final File root, final int fileSizeLimit, final int directoryBitSize) throws IOException, InterruptedException {

        if (isFileStorage32(root, directoryBitSize)) {
            return;
        }

        final File old = new File(root.getPath() + ".old");
        if (!root.renameTo(old)) {
            throw new IOException("Cannot rename " + root.getPath() + ".");
        }

        try (final FileStorage64 storage64 = new FileStorage64(old, fileSizeLimit, directoryBitSize);
                final FileStorage32 storage32 = new FileStorage32(root, fileSizeLimit, directoryBitSize);) {

            storage64.registerChunk(0L, SimpleBoardChunk.class, SimpleBoardChunk.getParser(), SimpleBoardChunk.Id.class, SimpleBoardChunk.Id.getParser());
            storage64.registerChunk(1L, ThreadChunk.class, ThreadChunk.getParser(), ThreadChunk.Id.class, ThreadChunk.Id.getParser());
            storage64.registerChunk(2L, OrderingBoardChunk.class, OrderingBoardChunk.getParser(), OrderingBoardChunk.Id.class,
                    OrderingBoardChunk.Id.getParser());

            storage32.registerChunk(0L, SimpleBoardChunk.class, SimpleBoardChunk.getParser(), SimpleBoardChunk.Id.class, SimpleBoardChunk.Id.getParser());
            storage32.registerChunk(1L, ThreadChunk.class, ThreadChunk.getParser(), ThreadChunk.Id.class, ThreadChunk.Id.getParser());
            storage32.registerChunk(2L, OrderingBoardChunk.class, OrderingBoardChunk.getParser(), OrderingBoardChunk.Id.class,
                    OrderingBoardChunk.Id.getParser());

            for (final Chunk.Id<?> id : storage64.getIndices(Address.ZERO, Address.MAX).keySet()) {
                final Chunk chunk;
                try {
                    chunk = storage64.read(id);
                } catch (MyRuleException | IOException e) {
                    LOG.log(Level.WARNING, id + " の読み込みに失敗しましたが、無視します。");
                    continue;
                }
                if (chunk == null) {
                    LOG.log(Level.WARNING, id + " がありませんでしたが、無視します。");
                    continue;
                }

                try {
                    storage32.write(chunk);
                } catch (final IOException e) {
                    LOG.log(Level.WARNING, id + " の書き込みに失敗しましたが、無視します。");
                    continue;
                }

                LOG.log(Level.INFO, id + " を変換しました。");
            }
        }
    }

    private static boolean isFileStorage32(final File root, final int directoryBitSize) {

        final List<String> names = new ArrayList<>();

        if (directoryBitSize == 0) {
            names.addAll(Arrays.asList(root.list()));
        } else {
            final File[] dirs = root.listFiles();
            if (dirs == null || dirs.length <= 0) {
                return true;
            }

            for (final File dir : dirs) {
                names.add(dir.getName());
                names.addAll(Arrays.asList(dir.list()));
            }
        }

        for (final String name : names) {
            if (name.equals("%%trash%%")) {
                continue;
            }

            try {
                Base32.fromBase32(name);
            } catch (final MyRuleException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * FileStorage64 を FileStorage32 に変換する。
     * @param args 1 番目が変換するディレクトリ。2 番目がディレクトリ用ビット数。
     * @throws IOException 読み書き異常
     * @throws InterruptedException 割り込まれた場合
     */
    public static void main(final String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.err.println("Arguments: {ROOT} [{DIR_BIT}]");
            System.exit(1);
        }
        final int directoryBitSize;
        if (args.length >= 2) {
            directoryBitSize = Integer.parseInt(args[1]);
        } else {
            directoryBitSize = 8;
        }

        convert(new File(args[0]), Integer.MAX_VALUE, directoryBitSize);
    }

}
