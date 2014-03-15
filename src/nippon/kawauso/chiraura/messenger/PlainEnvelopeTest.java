package nippon.kawauso.chiraura.messenger;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;

/**
 * @author chirauraNoSakusha
 */
public final class PlainEnvelopeTest extends BytesConvertibleTest<PlainEnvelope> {

    private final TypeRegistry<Message> registry;

    /**
     * 初期化。
     */
    public PlainEnvelopeTest() {
        this.registry = TypeRegistries.newRegistry();
        RegistryInitializer.init(this.registry);
    }

    @Override
    protected PlainEnvelope[] getInstances() {
        final List<PlainEnvelope> list = new ArrayList<>();
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage("いろはにほへと"));
            list.add((new PlainEnvelope(mail, this.registry)));
        }
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage(1234));
            list.add((new PlainEnvelope(mail, this.registry)));
        }
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage("ちりぬるを"));
            list.add((new PlainEnvelope(mail, this.registry)));
        }
        return list.toArray(new PlainEnvelope[0]);
    }

    @Override
    protected BytesConvertible.Parser<PlainEnvelope> getParser() {
        return PlainEnvelope.getParser(this.registry);
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

    @Override
    protected PlainEnvelope getInstance(final int seed) {
        final List<Message> mail = new ArrayList<>(1);
        mail.add(new TestMessage(seed));
        return new PlainEnvelope(mail, this.registry);
    }
}
