package nippon.kawauso.chiraura.storage;

import java.io.File;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.converter.Base32;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 操作毎にファイルを読み書きする倉庫。
 * @author chirauraNoSakusha
 */
final class FileStorage32 extends FileStorage {

    /*
     * データ片は、その論理位置を Base32 エンコードした名前のファイルに保存する。
     */

    FileStorage32(final File root, final int fileSizeLimit, final int directoryBitSize) {
        super(root, fileSizeLimit, directoryBitSize);

        final int maxDirectoryBitSize = (Address.SIZE / 5) * 5;// ディレクトリ名が論理位置以外の影響を受けない長さ。
        if (maxDirectoryBitSize < directoryBitSize) {
            throw new IllegalArgumentException("Too large directory bit size ( " + directoryBitSize + " ) not in [ 1, " + maxDirectoryBitSize + " ].");
        }
    }

    @Override
    String toFileString(final byte[] bytes) {
        return Base32.toBase32(bytes);
    }

    @Override
    byte[] fromFileString(final String string) throws MyRuleException {
        return Base32.fromBase32(string);
    }

}
