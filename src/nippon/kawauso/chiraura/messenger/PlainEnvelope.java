package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConversion;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 何もしない封筒。
 * 最小のメッセージは1バイトの0。
 * 以下の形式のバイト列と相互変換できる。
 *
 * <pre>
 * {要素数}
 * {要素種別1}{要素サイズ1}
 * {要素種別2}{要素サイズ2}
 * ...
 * {要素1}
 * {要素2}
 * ...
 * </pre>
 * @author chirauraNoSakusha
 */
final class PlainEnvelope implements Envelope {

    private static final class Index implements BytesConvertible {
        long id;
        int size;

        private Index(final long id, final int size) {
            this.id = id;
            this.size = size;
        }

        @Override
        public int byteSize() {
            return BytesConversion.byteSize("li", this.id, this.size);
        }

        @Override
        public int toStream(final OutputStream output) throws IOException {
            return BytesConversion.toStream(output, "li", this.id, this.size);
        }

        private static BytesConvertible.Parser<Index> getParser() {
            return new BytesConvertible.Parser<Index>() {
                @Override
                public int fromStream(final InputStream input, final int maxByteSize, final List<? super Index> output) throws MyRuleException, IOException {
                    final long[] id = new long[1];
                    final int[] size = new int[1];
                    final int len = BytesConversion.fromStream(input, maxByteSize, "li", id, size);
                    output.add(new Index(id[0], size[0]));
                    return len;
                }
            };
        }
    }

    private final List<Message> mail;
    private final TypeRegistry<Message> registry;

    PlainEnvelope(final List<Message> mail, final TypeRegistry<Message> registry) {
        if (mail == null) {
            throw new IllegalArgumentException("Null mail.");
        } else if (registry == null) {
            throw new IllegalArgumentException("Null registry.");
        }

        this.mail = mail;
        this.registry = registry;
    }

    @Override
    public List<Message> getMail() {
        return Collections.unmodifiableList(this.mail);
    }

    private List<Index> makeIndices() {
        final List<Index> indices = new ArrayList<>(this.mail.size());
        for (final Message message : this.mail) {
            indices.add(new Index(this.registry.getId(message), message.byteSize()));
        }
        return indices;
    }

    @Override
    public int byteSize() {
        int size = 0;
        final List<Index> indices = makeIndices();
        // 要素種別と要素サイズと要素。
        size += BytesConversion.byteSize("ao", indices);
        // 要素。
        for (final Index index : makeIndices()) {
            size += index.size;
        }
        return size;
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        int size = 0;
        // 要素種別と要素サイズ。
        size += BytesConversion.toStream(output, "ao", makeIndices());
        // 要素。
        for (final Message message : this.mail) {
            size += message.toStream(output);
        }
        return size;
    }

    static BytesConvertible.Parser<PlainEnvelope> getParser(final TypeRegistry<Message> registry) {
        return new BytesConvertible.Parser<PlainEnvelope>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super PlainEnvelope> output) throws MyRuleException,
                    IOException {
                int size = 0;
                // 要素種別と要素サイズ。
                final List<Index> indices = new ArrayList<>();
                size += BytesConversion.fromStream(input, maxByteSize - size, "ao", indices, Index.getParser());

                int sizeSum = 0;
                for (final Index index : indices) {
                    sizeSum += index.size;
                }
                if (maxByteSize < size + sizeSum) {
                    throw new MyRuleException("Too large required size ( " + (size + sizeSum) + " ) over limit ( " + maxByteSize + " ).");
                }

                // 要素。
                final List<Message> messages = new ArrayList<>(indices.size());
                for (final Index index : indices) {
                    final BytesConvertible.Parser<? extends Message> parser = registry.getParser(index.id);
                    if (parser == null) {
                        size += UnknownMessage.getParser(index.id).fromStream(input, index.size, messages);
                    } else {
                        size += parser.fromStream(input, index.size, messages);
                    }
                }
                output.add(new PlainEnvelope(messages, registry));
                return size;
            }
        };
    }

    @Override
    public int hashCode() {
        return this.mail.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PlainEnvelope)) {
            return false;
        }
        final PlainEnvelope other = (PlainEnvelope) obj;
        return this.mail.equals(other.mail);
    }

    @Override
    public String toString() {
        final StringBuilder buff = (new StringBuilder(this.getClass().getSimpleName())).append("[{");
        for (int i = 0; i < this.mail.size(); i++) {
            if (i != 0) {
                buff.append(", ");
            }
            buff.append(this.mail.get(i));
        }
        return buff.append("}]").toString();
    }

}
