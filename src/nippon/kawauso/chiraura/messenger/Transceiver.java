/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import nippon.kawauso.chiraura.lib.StreamFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;
import nippon.kawauso.chiraura.lib.http.Http;
import nippon.kawauso.chiraura.lib.http.InputStreamWrapper;

/**
 * 同期していないが、toStream やらを利用するスレッドを開始する前に register してしまえば、問題無い。
 * @author chirauraNoSakusha
 */
final class Transceiver {

    private static final Logger LOG = Logger.getLogger(Transceiver.class.getName());

    static interface Sealer<T extends Envelope> {
        T seal(List<Message> mail, Key encryptionKey);
    }

    static interface ParserGenerator<T extends Envelope> {
        BytesConvertible.Parser<T> getParser(final Key decryptionKey);
    }

    /**
     * 共有部分。
     * @author chirauraNoSakusha
     */
    static final class Share {

        private final int maxSize;
        private final boolean defaultHttp;
        private final Map<Class<? extends Envelope>, Long> typeToId;
        private final Map<Long, Sealer<?>> idToSealer;
        private final Map<Long, ParserGenerator<?>> idToParserGenerator;

        Share(final int maxSize, final boolean defaultHttp, final TypeRegistry<Message> registry) {
            if (maxSize < 0) {
                throw new IllegalArgumentException("Invalid max size ( " + maxSize + " ).");
            } else if (registry == null) {
                throw new IllegalArgumentException("Null registry.");
            }

            this.maxSize = maxSize;
            this.defaultHttp = defaultHttp;
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

    private final Share share;
    private final InputStreamWrapper input;
    private final OutputStream output;

    private static final int lineLimit = 2048;
    // HTTP ヘッダをつけるかどうか。
    private final AtomicBoolean http;
    // HTTP ヘッダをつけるときにクライアント側として振る舞うかどうか。
    private final boolean client;

    Transceiver(final Share share, final InputStream input, final OutputStream output, final boolean client) {
        if (share == null) {
            throw new IllegalArgumentException("Null share.");
        } else if (input == null) {
            throw new IllegalArgumentException("Null input.");
        } else if (output == null) {
            throw new IllegalArgumentException("Null output.");
        }
        this.share = share;
        this.input = new InputStreamWrapper(input, Http.HEADER_CHARSET, Http.SEPARATOR, lineLimit);
        this.output = output;

        this.http = new AtomicBoolean(share.defaultHttp);
        this.client = client;
    }

    /**
     * fromStream で読み込めるようにメッセージを書き込む。
     * @param output 書き込み先
     * @param mail メッセージ
     * @param type 使用する封筒の種別
     * @param encryptionKey 暗号鍵
     * @return 書き込みサイズ
     * @throws IOException 書き込みエラー
     */
    int toStream(final List<Message> mail, final Class<? extends Envelope> type, final Key encryptionKey) throws IOException {
        if (this.http.get()) {
            return httpToStream(mail, type, encryptionKey);
        } else {
            return bareToStream(mail, type, encryptionKey);
        }
    }

    private int httpToStream(final List<Message> mail, final Class<? extends Envelope> type, final Key encryptionKey) throws IOException {
        throw new UnsupportedOperationException("not yet implemented.");
    }

    private int bareToStream(final List<Message> mail, final Class<? extends Envelope> type, final Key encryptionKey) throws IOException {
        /*
         * {封書の種別}{封書のサイズ}{封書} の形で書き込む。
         */
        final Long id = this.share.typeToId.get(type);
        if (id == null) {
            throw new IllegalStateException("Not registered type ( " + type + " ).");
        }

        final Sealer<?> sealer = this.share.idToSealer.get(id);
        final Envelope envelope = sealer.seal(mail, encryptionKey);
        final int size = envelope.byteSize();
        if (size > this.share.maxSize) {
            throw new IllegalArgumentException("Invalid size ( " + size + " ) not in [ 0, " + this.share.maxSize + " ].");
        }
        return BytesConversion.toStream(this.output, "lio", id.longValue(), size, envelope);
    }

    /**
     * toStream で書き込まれたメッセージを読み込む。
     * @param input 読み込み元
     * @param decryptionKey 暗号鍵
     * @param sink 読み込んだメッセージの格納先
     * @return 読み込みサイズ
     * @throws MyRuleException 規約違反
     * @throws IOException 読み込みエラー
     */
    int fromStream(final Key decryptionKey, final List<? super Message> sink) throws MyRuleException, IOException {
        // 封筒の種別とサイズ。
        final long[] id = new long[1];
        final int size = BytesConversion.fromStream(this.input, this.share.maxSize, "l", id);
        if (id[0] == 71 || id[0] == 72 || id[0] == 80) {
            return size + httpFromStream(decryptionKey, sink);
        } else {
            return size + bareFromStream(id[0], decryptionKey, sink);
        }
    }

    private int httpFromStream(final Key decryptionKey, final List<? super Message> sink) throws MyRuleException, IOException {
        throw new UnsupportedOperationException("not yet implemented.");
    }

    private int bareFromStream(final long id, final Key decryptionKey, final List<? super Message> sink) throws MyRuleException, IOException {
        final int[] size = new int[1];
        final int headerSize = BytesConversion.fromStream(this.input, this.share.maxSize, "i", size);

        if (size[0] < 0 || this.share.maxSize < size[0]) {
            throw new MyRuleException("Invalid size ( " + size[0] + " ) not in [ 0, " + this.share.maxSize + " ].");
        }

        final ParserGenerator<?> parserGenerator = this.share.idToParserGenerator.get(id);
        if (parserGenerator != null) {
            final List<Envelope> envelope = new ArrayList<>(1);
            final int bodySize = parserGenerator.getParser(decryptionKey).fromStream(this.input, size[0], envelope);
            if (bodySize != size[0]) {
                throw new MyRuleException("Invalid mail size ( " + bodySize + " ) differing from declared size ( " + size[0] + " ).");
            }
            sink.addAll(envelope.get(0).getMail());
        } else {
            StreamFunctions.completeSkip(this.input, size[0]);
            LOG.log(Level.WARNING, "不明な封筒種別 ( {0} ) なので {1} バイト分無視します。", new Object[] { id, size[0] });
        }
        return headerSize + size[0];
    }

    boolean isEof() throws IOException {
        return StreamFunctions.isEof(this.input);
    }

    void flush() throws IOException {
        this.output.flush();
    }

}
