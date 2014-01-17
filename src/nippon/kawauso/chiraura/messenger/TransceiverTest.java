/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * @author chirauraNoSakusha
 */
public final class TransceiverTest extends BytesConvertibleTest<TransceiverElement> {

    private final Transceiver transceiver;
    private final Key commonKey;

    /**
     * 初期化。
     */
    public TransceiverTest() {
        final TypeRegistry<Message> registry = TypeRegistries.newRegistry();
        RegistryInitializer.init(registry);
        this.transceiver = new Transceiver(Integer.MAX_VALUE, registry);
        this.commonKey = CryptographicKeys.newCommonKey();
    }

    @Override
    protected TransceiverElement[] getInstances() {
        final List<TransceiverElement> list = new ArrayList<>();
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage("いろはにほへと"));
            list.add((new TransceiverElement(this.transceiver, mail, PlainEnvelope.class, this.commonKey)));
        }
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage(1234));
            list.add((new TransceiverElement(this.transceiver, mail, EncryptedWithRandomEnvelope.class, this.commonKey)));
        }
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage("ちりぬるを"));
            list.add((new TransceiverElement(this.transceiver, mail, EncryptedEnvelope.class, this.commonKey)));
        }
        return list.toArray(new TransceiverElement[0]);
    }

    @Override
    protected BytesConvertible.Parser<TransceiverElement> getParser() {
        return TransceiverElement.getParser(this.transceiver, this.commonKey);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

    private static final List<Class<? extends Envelope>> types;
    static {
        types = new ArrayList<>();
        types.add(PlainEnvelope.class);
        types.add(EncryptedEnvelope.class);
        types.add(EncryptedWithRandomEnvelope.class);
        types.add(GZippedEnvelope.class);
        types.add(GZippedEncryptedEnvelope.class);
    }

    @Override
    protected TransceiverElement getInstance(final int seed) {
        final List<Message> mail = new ArrayList<>(1);
        mail.add(new TestMessage(seed / types.size()));
        return new TransceiverElement(this.transceiver, mail, types.get(seed % types.size()), this.commonKey);
    }

}

final class TransceiverElement implements BytesConvertible {

    private final Transceiver transceiver;
    private final List<Message> mail;
    private final Class<? extends Envelope> type;
    private final Key key;

    TransceiverElement(final Transceiver transceiver, final List<Message> mail, final Class<? extends Envelope> type, final Key key) {
        this.transceiver = transceiver;
        this.mail = mail;
        this.type = type;
        this.key = key;
    }

    @Override
    public int byteSize() {
        final ByteArrayOutputStream buff = new ByteArrayOutputStream();
        try {
            this.transceiver.toStream(buff, this.mail, this.type, this.key);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return buff.size();
    }

    @Override
    public int toStream(final OutputStream output) throws IOException {
        return this.transceiver.toStream(output, this.mail, this.type, this.key);
    }

    static BytesConvertible.Parser<TransceiverElement> getParser(final Transceiver transceiver, final Key key) {
        return new BytesConvertible.Parser<TransceiverElement>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super TransceiverElement> output) throws MyRuleException,
                    IOException {
                final List<Message> mail = new ArrayList<>();
                final int size = transceiver.fromStream(input, key, mail);
                output.add(new TransceiverElement(transceiver, mail, null, key));
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

}
