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
public final class GZippedEnvelopeTest extends BytesConvertibleTest<GZippedEnvelope> {

    private final TypeRegistry<Message> registry;

    /**
     * 初期化。
     */
    public GZippedEnvelopeTest() {
        this.registry = TypeRegistries.newRegistry();
        RegistryInitializer.init(this.registry);
    }

    @Override
    protected GZippedEnvelope[] getInstances() {
        final List<GZippedEnvelope> list = new ArrayList<>();
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage("いろはにほへと"));
            list.add((new GZippedEnvelope(mail, this.registry)));
        }
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage(1234));
            list.add((new GZippedEnvelope(mail, this.registry)));
        }
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage("ちりぬるを"));
            list.add((new GZippedEnvelope(mail, this.registry)));
        }
        return list.toArray(new GZippedEnvelope[0]);
    }

    @Override
    protected BytesConvertible.Parser<GZippedEnvelope> getParser() {
        return GZippedEnvelope.getParser(this.registry);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

    @Override
    protected GZippedEnvelope getInstance(final int seed) {
        final List<Message> mail = new ArrayList<>(1);
        mail.add(new TestMessage(seed));
        return new GZippedEnvelope(mail, this.registry);
    }

}
