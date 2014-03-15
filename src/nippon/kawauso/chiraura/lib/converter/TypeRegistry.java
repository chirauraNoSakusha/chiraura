package nippon.kawauso.chiraura.lib.converter;


/**
 * @author chirauraNoSakusha
 * @param <T> 登録できるクラスの範囲
 */
public interface TypeRegistry<T> {

    /**
     * クラスを登録する。
     * @param <S> 登録するクラス
     * @param id 固有の登録番号
     * @param type 登録するクラス
     * @param parser 復号器
     */
    public <S extends T> void register(final long id, final Class<S> type, final BytesConvertible.Parser<? extends S> parser);

    /**
     * 登録番号を返す。
     * @param type 対象のクラス
     * @return 登録番号
     * @throws IllegalStateException 登録されていない場合
     */
    public long getId(final Class<? extends T> type);

    /**
     * 登録番号を返す。
     * @param instance 対象クラスのインスタンス
     * @return 登録番号
     * @throws IllegalStateException 登録されていない場合
     */
    public long getId(final T instance);

    /**
     * 復号器を返す。
     * @param id 登録番号
     * @return 復号器
     *         登録されていない場合は null
     */
    public BytesConvertible.Parser<? extends T> getParser(final long id);

    /**
     * 復号器を返す。
     * @param type 対象のクラス
     * @return 復号器
     *         登録されていない場合は null
     */
    public BytesConvertible.Parser<? extends T> getParser(final Class<? extends T> type);

}
