package nippon.kawauso.chiraura.closet;

import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * 差分から構成されるデータ片。
 * 塵も積もれば山となる。
 * @author chirauraNoSakusha
 */
public interface Mountain extends Chunk {

    /**
     * 差分。
     * 塵も積もれば山となる。
     * @author chirauraNoSakusha
     * @param <T> 適用先データ片の型
     */
    public static interface Dust<T extends Mountain> extends BytesConvertible {
        /**
         * 適用先データ片の型を返す。
         * @return 適用先データ片の型
         */
        public Class<T> getMountainClass();

        @Override
        public int hashCode();

        @Override
        public boolean equals(Object obj);
    }

    @Override
    public Chunk.Id<? extends Mountain> getId();

    /**
     * 複製する。
     * @return 複製
     */
    public Mountain copy();

    /**
     * 最初の日時を返す。
     * @return 最初の日時
     */
    public long getFirstDate();

    /**
     * ある時以降の差分を取得する。
     * @param date 基準時
     * @return 基準時以降の差分
     */
    public List<? extends Mountain.Dust<?>> getDiffsAfter(long date);

    /**
     * 差分が適用できるかどうか検査する。
     * @param diff 差分
     * @return 適用できる場合のみ true
     */
    public boolean patchable(Dust<?> diff);

    /**
     * 差分を適用する。
     * @param diff 差分
     * @return 適用できた場合のみ true
     */
    public boolean patch(Dust<?> diff);

    /**
     * 差分としては現れない基礎の部分が同じかどうか検査する。
     * @param o 比較対象
     * @return 同じ場合のみ true
     */
    public boolean baseEquals(Mountain o);

    @Override
    public int hashCode();

    @Override
    public boolean equals(Object obj);

}
