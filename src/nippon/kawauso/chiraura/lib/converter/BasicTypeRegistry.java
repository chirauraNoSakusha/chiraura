package nippon.kawauso.chiraura.lib.converter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author chirauraNoSakusha
 */
final class BasicTypeRegistry<T> implements TypeRegistry<T> {

    private final Map<Class<? extends T>, Long> typeToId;
    private final Map<Long, BytesConvertible.Parser<? extends T>> idToParser;

    BasicTypeRegistry() {
        this.typeToId = new HashMap<>();
        this.idToParser = new HashMap<>();
    }

    @Override
    public <S extends T> void register(final long id, final Class<S> type, final BytesConvertible.Parser<? extends S> parser) {
        final Long oldType = this.typeToId.put(type, id);
        if (oldType != null) {
            throw new IllegalStateException("Class ( " + type.getName() + " ) overlap.");
        }
        final BytesConvertible.Parser<?> oldParser = this.idToParser.put(id, parser);
        if (oldParser != null) {
            throw new IllegalStateException("Type ( " + id + " ) overlap.");
        }
    }

    @Override
    public long getId(final Class<? extends T> type) {
        final Long id = this.typeToId.get(type);
        if (id == null) {
            throw new IllegalStateException("Not registered type ( " + type.getName() + " ).");
        }
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public long getId(final T instance) {
        return getId((Class<? extends T>) instance.getClass());
    }

    @Override
    public BytesConvertible.Parser<? extends T> getParser(final long id) {
        return this.idToParser.get(id);
    }

    @Override
    public BytesConvertible.Parser<? extends T> getParser(final Class<? extends T> type) {
        final Long id = this.typeToId.get(type);
        if (id == null) {
            return null;
        }
        return getParser(id);
    }

}
