package nippon.kawauso.chiraura.storage;

import java.io.File;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.converter.Base64;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 操作毎にファイルを読み書きする倉庫。
 * @author chirauraNoSakusha
 */
final class FileStorage64 extends FileStorage {

    /*
     * データ片は、その論理位置を BASE64 エンコードした名前のファイルに保存する。
     */

    FileStorage64(final File root, final int fileSizeLimit, final int directoryBitSize) {
        super(root, fileSizeLimit, directoryBitSize);

        final int maxDirectoryBitSize = (Address.SIZE / 6) * 6;// ディレクトリ名が論理位置以外の影響を受けない長さ。
        if (maxDirectoryBitSize < directoryBitSize) {
            throw new IllegalArgumentException("Too large directory bit size ( " + directoryBitSize + " ) not in [ 1, " + maxDirectoryBitSize + " ].");
        }
    }

    /*
     * ファイル名に使う記号。
     * '/' は Unix のディレクトリ区切りなので '+' に。
     * '-' はファイル名の先頭だとコマンドラインオプションと混同するので '_' に。
     */
    private static final char BASE64_63 = '+';
    private static final char BASE64_64 = '_';

    @Override
    String toFileString(final byte[] bytes) {
        return Base64.toBase64(bytes, BASE64_63, BASE64_64);
    }

    @Override
    byte[] fromFileString(final String string) throws MyRuleException {
        return Base64.fromBase64(string, BASE64_63, BASE64_64);
    }

}
