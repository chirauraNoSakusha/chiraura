package nippon.kawauso.chiraura.lib.converter;


/**
 * @author chirauraNoSakusha
 */
public final class TypeRegistries {

    // インスタンス化防止。
    private TypeRegistries() {}

    /**
     * 登記簿を作成する。
     * @param <T> 登録する型
     * @return 白紙の登記簿
     */
    public static <T> TypeRegistry<T> newRegistry() {
        return new BasicTypeRegistry<>();
    }

    /**
     * 見るだけの登記簿を作成する。
     * @param <T> 登録される型
     * @param base 元にする登記簿
     * @return base と連動するが、登録不可の登記簿
     */
    public static <T> TypeRegistry<T> unregisterableRegistry(final TypeRegistry<T> base) {
        return new TypeRegistry<T>() {
            @Override
            public <S extends T> void register(final long id, final Class<S> type, final BytesConvertible.Parser<? extends S> parser) {
                throw new UnsupportedOperationException("Not supported.");
            }

            @Override
            public long getId(final Class<? extends T> type) {
                return base.getId(type);
            }

            @Override
            public long getId(final T instance) {
                return base.getId(instance);
            }

            @Override
            public BytesConvertible.Parser<? extends T> getParser(final long id) {
                return base.getParser(id);
            }

            @Override
            public BytesConvertible.Parser<? extends T> getParser(final Class<? extends T> type) {
                return base.getParser(type);
            }
        };
    }

}
