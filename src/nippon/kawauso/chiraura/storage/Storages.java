/**
 * 
 */
package nippon.kawauso.chiraura.storage;

import java.io.File;

/**
 * データ片倉庫の作成など。
 * @author chirauraNoSakusha
 */
public final class Storages {

    // インスタンス化防止。
    private Storages() {}

    /**
     * データ片倉庫を作成する。
     * @param root ルートディレクトリ
     * @param chunkSizeLimit データ片の制限サイズ
     * @param directoryBitSize ディレクトリに使うビット数
     * @param chunkCacheCapacity データ片をキャッシュする数
     * @param indexCacheCapacity データ片の概要をキャッシュする数
     * @param rangeCacheCapacity データ片の概要の範囲取得結果をキャッシュする数
     * @return データ片倉庫
     */
    public static Storage newInstance(final File root, final int chunkSizeLimit, final int directoryBitSize, final int chunkCacheCapacity,
            final int indexCacheCapacity, final int rangeCacheCapacity) {
        Storage instance = new FileStorage(root, chunkSizeLimit, directoryBitSize);
        instance = new WriteCachingStorage(instance, chunkCacheCapacity);
        instance = new RangeIndexingStorage(instance, rangeCacheCapacity);
        instance = new IndexingStorage(instance, indexCacheCapacity);
        return instance;
    }

}
