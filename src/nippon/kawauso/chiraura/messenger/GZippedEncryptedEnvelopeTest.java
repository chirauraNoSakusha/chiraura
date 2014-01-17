/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;

/**
 * @author chirauraNoSakusha
 */
public final class GZippedEncryptedEnvelopeTest extends BytesConvertibleTest<GZippedEncryptedEnvelope> {

    private final TypeRegistry<Message> registry;
    private final Key commonKey;

    /**
     * 初期化。
     */
    public GZippedEncryptedEnvelopeTest() {
        this.registry = TypeRegistries.newRegistry();
        RegistryInitializer.init(this.registry);
        this.commonKey = CryptographicKeys.newCommonKey();
    }

    @Override
    protected GZippedEncryptedEnvelope[] getInstances() {
        final List<GZippedEncryptedEnvelope> list = new ArrayList<>();
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage("いろはにほへと"));
            list.add((new GZippedEncryptedEnvelope(mail, this.registry, this.commonKey)));
        }
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage(1234));
            list.add((new GZippedEncryptedEnvelope(mail, this.registry, this.commonKey)));
        }
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage("ちりぬるを"));
            list.add((new GZippedEncryptedEnvelope(mail, this.registry, this.commonKey)));
        }
        return list.toArray(new GZippedEncryptedEnvelope[0]);
    }

    @Override
    protected BytesConvertible.Parser<GZippedEncryptedEnvelope> getParser() {
        return GZippedEncryptedEnvelope.getParser(this.registry, this.commonKey);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

    @Override
    protected GZippedEncryptedEnvelope getInstance(final int seed) {
        final List<Message> mail = new ArrayList<>(1);
        mail.add(new TestMessage(seed));
        return new GZippedEncryptedEnvelope(mail, this.registry, this.commonKey);
    }

}
