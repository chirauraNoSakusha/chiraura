/**
 * 
 */
package nippon.kawauso.chiraura.storage;

import java.io.IOException;
import java.util.Map;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * データ片の倉庫。
 * データ片のインスタンスは不変と想定する。
 * よって、データ片を書き変える場合、変更を適用した複製で上書きする。
 * lock すると、その実行プロセス以外は、少くとも contains や read の結果を書き換える操作に入れなくなり、待機させられる。
 * データ片の中身を参照してそれを書き変えるような場合、lock は必須。
 * また、内部で操作対象のデータ片の lock が行われる可能性があるため、他のデータ片を lock したままだとデッドロックの可能性あり。
 * @author chirauraNoSakusha
 */
public interface Storage extends AutoCloseable {

    /**
     * データ片の概要。
     * @author chirauraNoSakusha
     */
    public static interface Index {

        /*
         * バイト数は返さない。
         * なぜなら、更新日時のような単調増加ではないので、新旧判定には使えないし、
         * 同一性判定ならハッシュ値の方が良いから。
         */

        /**
         * データ片の識別子を返す。
         * データ片の getId と等しい値を返す。
         * @return データ片の識別子
         */
        public Chunk.Id<?> getId();

        /**
         * データ片の更新日時を返す。
         * データ片の getDate と等しい値を返す。
         * @return データ片の更新日時
         */
        public long getDate();

        /**
         * データ片の内容のハッシュ値を返す。
         * データ片の getHashValue と等しい値を返す。
         * @return データ片の内容のハッシュ値
         */
        public HashValue getHashValue();

        /**
         * 識別子が等しいかどうかを返す
         * @param o 比較相手
         * @return 識別子が等しい場合のみ true
         */
        @Override
        public boolean equals(Object o);
    }

    /**
     * 使用するデータ片の型を登録する。
     * @param <C> 登録するデータ片の型
     * @param <I> 登録するデータ片の識別子の型
     * @param type 割り当てる固有の番号
     * @param chunkClass 登録するデータ片の型
     * @param chunkParser 登録するデータ片の復号器
     * @param idClass 登録するデータ片の識別子の型
     * @param idParser 登録するデータ片の識別子の復号器
     */
    public <C extends Chunk, I extends Chunk.Id<C>> void registerChunk(long type, Class<C> chunkClass, BytesConvertible.Parser<? extends C> chunkParser,
            Class<I> idClass, BytesConvertible.Parser<? extends I> idParser);

    /**
     * 登録不可のデータ片の登記簿を返す。
     * @return 登録不可のデータ片の登記簿
     */
    public TypeRegistry<Chunk> getChunkRegistry();

    /**
     * 登録不可のデータ片の識別子の登記簿を返す。
     * @return 登録不可のデータ片の識別子の登記簿
     */
    public TypeRegistry<Chunk.Id<?>> getIdRegistry();

    /**
     * データ片をロックする。
     * ロックできるまで待機する。
     * @param id データ片の識別子
     * @throws InterruptedException インタラプトされた場合
     */
    public void lock(final Chunk.Id<?> id) throws InterruptedException;

    /**
     * データ片をロックする。
     * ロックできない場合は諦める。
     * @param id データ片の識別子
     * @return ロックできたら true
     */
    public boolean tryLock(Chunk.Id<?> id);

    /**
     * データ片のロックを外す。
     * @param id データ片の識別子
     */
    public void unlock(final Chunk.Id<?> id);

    /**
     * データ片の存在検査。
     * @param id データ片の識別子
     * @return 存在すればtrue
     * @throws IOException デバイスからの読み込みエラー
     * @throws InterruptedException ロックを待っている間にインタラプトされた場合
     */
    public boolean contains(final Chunk.Id<?> id) throws IOException, InterruptedException;

    /**
     * データ片の概要を取得する。
     * @param id データ片の識別子
     * @return データ片の概要。
     *         データ片が存在しない場合は null
     * @throws MyRuleException デバイスからの読み込み時に不正な状態を検知した場合
     * @throws IOException デバイスからの読み込みエラー
     * @throws InterruptedException ロックを待っている間にインタラプトされた場合
     */
    public Index getIndex(Chunk.Id<?> id) throws MyRuleException, IOException, InterruptedException;

    /**
     * データ片の概要を列挙する。
     * 列挙された概要はその時点でのものなので、それ以降の変更等は反映されないし、
     * 自由に操作して良い。
     * @param min 論理位置の先頭
     * @param max 論理位置の末尾 (min 以上)
     * @return min から max までの全所持データ片の概要
     * @throws IOException デバイスからの読み込みエラー
     * @throws InterruptedException ロックを待っている間にインタラプトされた場合
     */
    public Map<Chunk.Id<?>, Index> getIndices(Address min, Address max) throws IOException, InterruptedException;

    /**
     * データ片を読む。
     * @param <T> データ片の型
     * @param id データ片の識別子
     * @return データ片。
     *         存在しない場合は null
     * @throws MyRuleException デバイスからの読み込み時に不正な状態を検知した場合
     * @throws IOException デバイスからの読み込みエラー
     * @throws InterruptedException ロックを待っている間にインタラプトされた場合
     */
    public <T extends Chunk> T read(final Chunk.Id<T> id) throws MyRuleException, IOException, InterruptedException;

    /**
     * データ片を書く。
     * 既に等しいデータ片がある場合は何もしない。
     * @param chunk データ片
     * @return 書き込んだ場合のみ true
     * @throws IOException デバイスへの書き込みエラー
     * @throws InterruptedException ロックを待っている間にインタラプトされた場合
     */
    public boolean write(final Chunk chunk) throws IOException, InterruptedException;

    /**
     * データ片を書く。
     * 既にある場合は上書きする。
     * 既存検査が無い分、書き込む場合には write より高速。
     * @param chunk データ片
     * @throws IOException デバイスへの書き込みエラー
     * @throws InterruptedException ロックを待っている間にインタラプトされた場合
     */
    public void forceWrite(final Chunk chunk) throws IOException, InterruptedException;

    /**
     * データ片を消す。
     * @param id データ片の識別子
     * @return 消した場合 true。
     *         存在しなかった場合は false
     * @throws IOException デバイスへの書き込みエラー
     * @throws InterruptedException ロックを待っている間にインタラプトされた場合
     */
    public boolean delete(Chunk.Id<?> id) throws IOException, InterruptedException;

    @Override
    public void close() throws MyRuleException, InterruptedException, IOException;

}
