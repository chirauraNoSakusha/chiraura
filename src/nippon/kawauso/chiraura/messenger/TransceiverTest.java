/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * input を中でラップする関係で 2 つに分けなくてはならない。
 * @author chirauraNoSakusha
 */
public final class TransceiverTest extends BytesConvertibleTest<TransceiverElement> {

    private static final boolean http = Global.useHttpWrapper();

    private final Transceiver.Share transceiverShare;
    private final Key commonKey;

    /**
     * 初期化。
     */
    public TransceiverTest() {
        final TypeRegistry<Message> registry = TypeRegistries.newRegistry();
        RegistryInitializer.init(registry);
        this.transceiverShare = new Transceiver.Share(Integer.MAX_VALUE, http, registry);
        this.commonKey = CryptographicKeys.newCommonKey();
    }

    @Override
    protected TransceiverElement[] getInstances() {
        final List<TransceiverElement> list = new ArrayList<>();
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage("いろはにほへと"));
            list.add((new TransceiverElement(this.transceiverShare, mail, PlainEnvelope.class, this.commonKey)));
        }
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage(1234));
            list.add((new TransceiverElement(this.transceiverShare, mail, EncryptedWithRandomEnvelope.class, this.commonKey)));
        }
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage("ちりぬるを"));
            list.add((new TransceiverElement(this.transceiverShare, mail, EncryptedEnvelope.class, this.commonKey)));
        }
        return list.toArray(new TransceiverElement[0]);
    }

    @Override
    protected BytesConvertible.Parser<TransceiverElement> getParser() {
        return TransceiverElement.getParser(this.transceiverShare, this.commonKey);
    }

    @Override
    protected int getNumOfLoops() {
        return 0;
    }

    @Override
    protected TransceiverElement getInstance(final int seed) {
        return null;
    }

}

final class TransceiverElement implements BytesConvertible {

    private final Transceiver.Share transceiverShare;
    private final List<Message> mail;
    private final Class<? extends Envelope> type;
    private final Key key;

    TransceiverElement(final Transceiver.Share transceiverShare, final List<Message> mail, final Class<? extends Envelope> type, final Key key) {
        this.transceiverShare = transceiverShare;
        this.mail = mail;
        this.type = type;
        this.key = key;
    }

    @Override
    public int byteSize() {
        final ByteArrayOutputStream buff = new ByteArrayOutputStream();
        try {
            new Transceiver(this.transceiverShare, new ByteArrayInputStream(new byte[0]), buff, null).toStream(this.mail, this.type, this.key);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return buff.size();
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return new Transceiver(this.transceiverShare, new ByteArrayInputStream(new byte[0]), output, null).toStream(this.mail, this.type, this.key);
    }

    private static Transceiver transceiver = null;

    static BytesConvertible.Parser<TransceiverElement> getParser(final Transceiver.Share transceiverShare, final Key key) {
        return new BytesConvertible.Parser<TransceiverElement>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super TransceiverElement> output) throws MyRuleException,
                    IOException {
                final List<Message> mail = new ArrayList<>();
                if (transceiver == null) {
                    transceiver = new Transceiver(transceiverShare, input, new ByteArrayOutputStream(0), null);
                }
                final int size = transceiver.fromStream(key, mail);
                output.add(new TransceiverElement(transceiverShare, mail, null, key));
                return size;
            }
        };
    }

    static BytesConvertible.Parser<TransceiverElement> getLoopParser(final Transceiver.Share transceiverShare, final Key key) {
        return new BytesConvertible.Parser<TransceiverElement>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super TransceiverElement> output) throws MyRuleException,
                    IOException {
                final List<Message> mail = new ArrayList<>();
                final int size = (new Transceiver(transceiverShare, input, new ByteArrayOutputStream(0), null)).fromStream(key, mail);
                output.add(new TransceiverElement(transceiverShare, mail, null, key));
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
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TransceiverElement)) {
            return false;
        }
        final TransceiverElement other = (TransceiverElement) obj;
        return this.mail.equals(other.mail);
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.transceiverShare)
                .append(", ").append(this.mail)
                .append(", ").append(this.type)
                .append(']').toString();
    }

}
