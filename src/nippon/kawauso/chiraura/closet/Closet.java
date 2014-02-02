/**
 * 
 */
package nippon.kawauso.chiraura.closet;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.storage.Chunk;

/**
 * 四次元押入。
 * @author chirauraNoSakusha
 */
public interface Closet extends AutoCloseable {

    /**
     * 使用するデータ片の型を登録する。
     * データ片が差分形式ならば、別の registerChunk を用いること。
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
     * 使用する差分形式のデータ片の型を登録する。
     * @param <C> 登録するデータ片の型
     * @param <I> 登録するデータ片の識別子の型
     * @param <D> 登録するデータ片の差分の型
     * @param type 割り当てる固有の番号
     * @param chunkClass 登録するデータ片の型
     * @param chunkParser 登録するデータ片の復号器
     * @param idClass 登録するデータ片の識別子の型
     * @param idParser 登録するデータ片の識別子の復号器
     * @param diffClass 登録するデータ片の差分の型
     * @param diffParser 登録するデータ片の差分の復号器
     */
    public <C extends Mountain, I extends Chunk.Id<C>, D extends Mountain.Dust<C>> void registerChunk(long type, Class<C> chunkClass,
            BytesConvertible.Parser<? extends C> chunkParser, Class<I> idClass, BytesConvertible.Parser<? extends I> idParser, Class<D> diffClass,
            BytesConvertible.Parser<? extends D> diffParser);

    /**
     * データ片を取得する。
     * @param <T> データ片の型
     * @param id 取得するデータ片の識別子
     * @param timeout 制限時間
     * @return データ片。
     *         取得できなかった場合、時間切れだった場合は null
     * @throws InterruptedException 割り込まれた場合
     */
    public <T extends Chunk> T getChunk(Chunk.Id<T> id, long timeout) throws InterruptedException;

    /**
     * データ片を取得する。
     * すぐ取得できない分は取得しない。
     * @param <T> データ片の型
     * @param id 取得するデータ片の識別子
     * @return データ片。
     *         取得できなかった場合は null
     * @throws InterruptedException 割り込まれた場合
     */
    public <T extends Chunk> T getChunkImmediately(Chunk.Id<T> id) throws InterruptedException;

    /**
     * データ片を追加する。
     * @param chunk 追加するデータ片
     * @param timeout 制限時間
     * @return 追加できた場合 true。
     *         既にある場合、時間切れだった場合は false
     * @throws InterruptedException 割り込まれた場合
     */
    public boolean addChunk(Chunk chunk, long timeout) throws InterruptedException;

    /**
     * 差分適用の結果。
     * @author chirauraNoSakusha
     * @param <T> 適用先データ片の型
     */
    public static interface PatchResult<T extends Mountain> {

        /**
         * 異常で失敗したかどうか。
         * @return 異常で失敗したら true
         */
        public boolean isGivenUp();

        /**
         * 適用先が無かったかどうか。
         * @return 適用先がなかったら true
         */
        public boolean isNotFound();

        /**
         * 適用できたかどうか。
         * @return 適用できたら true
         */
        public boolean isSuccess();

        /**
         * 適用操作後のデータ片。
         * @return 操作後のデータ片。
         *         isGivenUp か isNotFound が true の場合は未定義
         */
        public T getChunk();
    }

    /**
     * 差分を適用する。
     * @param <T> データ片の型
     * @param id 適用先のデータ片の識別子
     * @param diff 差分
     * @param timeout 制限時間
     * @return 結果
     * @throws InterruptedException 割り込まれた場合
     */
    public <T extends Mountain> PatchResult<T> patchChunk(Chunk.Id<T> id, Mountain.Dust<T> diff, long timeout) throws InterruptedException;

    /**
     * 差分適用の結果。
     * @author chirauraNoSakusha
     * @param <T> 適用先データ片の型
     */
    public static interface PatchOrAddResult<T extends Mountain> {

        /**
         * 異常で失敗したかどうか。
         * @return 異常で失敗したら true
         */
        public boolean isGivenUp();

        /**
         * 適用操作後のデータ片。
         * @return 操作後のデータ片。
         *         isGivenUp が true の場合は未定義
         */
        public T getChunk();
    }

    /**
     * 差分を適用する。
     * @param <T> データ片の型
     * @param chunk データ片
     * @param timeout 制限時間
     * @return 結果
     * @throws InterruptedException 割り込まれた場合
     */
    public <T extends Mountain> PatchOrAddResult<T> patchOrAddChunk(T chunk, long timeout) throws InterruptedException;

    /**
     * 異常通知を取り出す。
     * 無い場合は待つ。
     * @return 異常通知
     * @throws InterruptedException 割り込まれた場合
     */
    public ClosetReport takeError() throws InterruptedException;

    /**
     * 起動する。
     * @param executor 実行機
     */
    public void start(ExecutorService executor);

    @Override
    public void close() throws MyRuleException, InterruptedException, IOException;

}
