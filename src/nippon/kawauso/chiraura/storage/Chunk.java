package nippon.kawauso.chiraura.storage;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;

/**
 * データ片の型。
 * @author chirauraNoSakusha
 */
public interface Chunk extends BytesConvertible {

    /**
     * データ片の識別子の型。
     * Mountain をファイルとすれば、 Id はファイルパスに当たる。
     * @author chirauraNoSakusha
     * @param <T> 対応するデータ片の型
     */
    public static interface Id<T extends Chunk> extends BytesConvertible {

        /**
         * 対応するデータ片のクラス
         * @return データ片のクラス。
         */
        public Class<T> getChunkClass();

        /**
         * 対応するデータ片の論理位置を返す。
         * @return データ片の論理位置。
         */
        public Address getAddress();

        @Override
        public int hashCode();

        @Override
        public boolean equals(Object obj);
    }

    /**
     * 自身の識別子を返す。
     * @return 識別子
     */
    public Chunk.Id<?> getId();

    /**
     * 更新日時を返す。
     * @return 更新日時 (ミリ秒)
     */
    public long getDate();

    /**
     * 内容のハッシュ値を返す。
     * @return 内容のハッシュ値
     */
    public HashValue getHashValue();

    @Override
    public int hashCode();

    @Override
    public boolean equals(Object obj);

}
