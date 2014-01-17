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
public final class EncryptedEnvelopeTest extends BytesConvertibleTest<EncryptedEnvelope> {

    private final TypeRegistry<Message> registry;
    private final Key commonKey;

    /**
     * 初期化。
     */
    public EncryptedEnvelopeTest() {
        this.registry = TypeRegistries.newRegistry();
        RegistryInitializer.init(this.registry);
        this.commonKey = CryptographicKeys.newCommonKey();
    }

    @Override
    protected EncryptedEnvelope[] getInstances() {
        final List<EncryptedEnvelope> list = new ArrayList<>();
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage("いろはにほへと"));
            list.add((new EncryptedEnvelope(mail, this.registry, this.commonKey)));
        }
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage(1234));
            list.add((new EncryptedEnvelope(mail, this.registry, this.commonKey)));
        }
        {
            final List<Message> mail = new ArrayList<>();
            mail.add(new TestMessage("ちりぬるを"));
            list.add((new EncryptedEnvelope(mail, this.registry, this.commonKey)));
        }
        return list.toArray(new EncryptedEnvelope[0]);
    }

    @Override
    protected BytesConvertible.Parser<EncryptedEnvelope> getParser() {
        return EncryptedEnvelope.getParser(this.registry, this.commonKey);
    }

    @Override
    protected int getNumOfLoops() {
        return 100_000;
    }

    @Override
    protected EncryptedEnvelope getInstance(final int seed) {
        final List<Message> mail = new ArrayList<>(1);
        mail.add(new TestMessage(seed));
        return new EncryptedEnvelope(mail, this.registry, this.commonKey);
    }

    // // 以下、公開鍵暗号の場合のテスト
    // @Override
    // protected int getNumOfPerformanceLoops() {
    // return 1_000;
    // }
    //
    // private final KeyPair publicKeys = CryptographicKeys.newPublicKeyPair();
    //
    // @Override
    // protected EncryptedEnvelope getInstance(final int seed) throws Exception {
    // final List<Message> message = new ArrayList<>(1);
    // message.add(new IntCell(seed));
    // return new EncryptedEnvelope(message, this.publicKeys.getPublic());
    // }
    //
    // @Override
    // protected BytesConvertible.Parser<EncryptedEnvelope> getPerformanceParser(final int seed) {
    // return EncryptedEnvelope.getParser(this.publicKeys.getPrivate());
    // }

}
