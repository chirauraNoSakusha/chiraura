/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.security.Key;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;

/**
 * 送受信機の共有部分。
 * 同期していないが、toStream やらを利用するスレッドを開始する前に register してしまえば、問題無い。
 * @author chirauraNoSakusha
 */
final class TransceiverShare {

    static interface Sealer<T extends Envelope> {
        T seal(List<Message> mail, Key encryptionKey);
    }

    static interface ParserGenerator<T extends Envelope> {
        BytesConvertible.Parser<T> getParser(final Key decryptionKey);
    }

    final int maxSize;
    final Map<Class<? extends Envelope>, Long> typeToId;
    final Map<Long, Sealer<?>> idToSealer;
    final Map<Long, ParserGenerator<?>> idToParserGenerator;

    TransceiverShare(final int maxSize, final TypeRegistry<Message> registry) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("Invalid max size ( " + maxSize + " ).");
        } else if (registry == null) {
            throw new IllegalArgumentException("Null registry.");
        }

        this.maxSize = maxSize;
        this.typeToId = new HashMap<>();
        this.idToSealer = new HashMap<>();
        this.idToParserGenerator = new HashMap<>();

        // 平文の封書。
        register(0, PlainEnvelope.class, new Sealer<PlainEnvelope>() {
            @Override
            public PlainEnvelope seal(final List<Message> mail, final Key encryptionKey) {
                return new PlainEnvelope(mail, registry);
            }
        }, new ParserGenerator<PlainEnvelope>() {
            @Override
            public BytesConvertible.Parser<PlainEnvelope> getParser(final Key decryptionKey) {
                return PlainEnvelope.getParser(registry);
            }
        });

        // 与えられた鍵で暗号化する封書。
        register(1, EncryptedEnvelope.class, new Sealer<EncryptedEnvelope>() {
            @Override
            public EncryptedEnvelope seal(final List<Message> mail, final Key encryptionKey) {
                return new EncryptedEnvelope(mail, registry, encryptionKey);
            }
        }, new ParserGenerator<EncryptedEnvelope>() {
            @Override
            public BytesConvertible.Parser<EncryptedEnvelope> getParser(final Key decryptionKey) {
                return EncryptedEnvelope.getParser(registry, decryptionKey);
            }
        });

        // 与えられた鍵と乱数で暗号化する封書。
        register(2, EncryptedWithRandomEnvelope.class, new Sealer<EncryptedWithRandomEnvelope>() {
            @Override
            public EncryptedWithRandomEnvelope seal(final List<Message> mail, final Key encryptionKey) {
                return new EncryptedWithRandomEnvelope(mail, registry, encryptionKey);
            }
        }, new ParserGenerator<EncryptedWithRandomEnvelope>() {
            @Override
            public BytesConvertible.Parser<EncryptedWithRandomEnvelope> getParser(final Key decryptionKey) {
                return EncryptedWithRandomEnvelope.getParser(registry, decryptionKey);
            }
        });

        // gzip で圧縮する封筒。
        register(3, GZippedEnvelope.class, new Sealer<GZippedEnvelope>() {
            @Override
            public GZippedEnvelope seal(final List<Message> mail, final Key encryptionKey) {
                return new GZippedEnvelope(mail, registry);
            }
        }, new ParserGenerator<GZippedEnvelope>() {
            @Override
            public BytesConvertible.Parser<GZippedEnvelope> getParser(final Key decryptionKey) {
                return GZippedEnvelope.getParser(registry);
            }
        });

        // gzip で圧縮し、与えられた鍵で暗号化する封筒。
        register(4, GZippedEncryptedEnvelope.class, new Sealer<GZippedEncryptedEnvelope>() {
            @Override
            public GZippedEncryptedEnvelope seal(final List<Message> mail, final Key encryptionKey) {
                return new GZippedEncryptedEnvelope(mail, registry, encryptionKey);
            }
        }, new ParserGenerator<GZippedEncryptedEnvelope>() {
            @Override
            public BytesConvertible.Parser<GZippedEncryptedEnvelope> getParser(final Key decryptionKey) {
                return GZippedEncryptedEnvelope.getParser(registry, decryptionKey);
            }
        });

        // 71 番。予約済み。HTTP 偽装時の [G]ET で使う。
        // 72 番。予約済み。HTTP 偽装時の [H]TTP で使う。
        // 80 番。予約済み。HTTP 偽装時の [P]OST で使う。
    }

    private <T extends Envelope> void register(final long id, final Class<T> type, final Sealer<T> sealer, final ParserGenerator<T> parserGenerator) {
        final Long oldId = this.typeToId.put(type, id);
        if (oldId != null) {
            throw new IllegalStateException("Type ( " + type.getName() + " ) overlap.");
        }
        final Sealer<?> oldSealer = this.idToSealer.put(id, sealer);
        final ParserGenerator<?> oldParserGenerator = this.idToParserGenerator.put(id, parserGenerator);
        if (oldSealer != null || oldParserGenerator != null) {
            throw new IllegalStateException("Id ( " + id + " ) overlap.");
        }
    }

}
