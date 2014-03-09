/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.Global;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;
import nippon.kawauso.chiraura.lib.converter.TypeRegistries;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;

/**
 * @author chirauraNoSakusha
 */
public final class TransceiverLoopTest extends BytesConvertibleTest<TransceiverElement> {

    private static final boolean http = Global.useHttpWrapper();

    private final Transceiver.Share transceiverShare;
    private final Key commonKey;

    /**
     * 初期化。
     */
    public TransceiverLoopTest() {
        final TypeRegistry<Message> registry = TypeRegistries.newRegistry();
        RegistryInitializer.init(registry);
        this.transceiverShare = new Transceiver.Share(Integer.MAX_VALUE, http, registry);
        this.commonKey = CryptographicKeys.newCommonKey();
    }

    @Override
    protected TransceiverElement[] getInstances() {
        return new TransceiverElement[0];
    }

    @Override
    protected BytesConvertible.Parser<TransceiverElement> getParser() {
        return TransceiverElement.getLoopParser(this.transceiverShare, this.commonKey);
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
        mail.add(new TestMessage(seed));
        return new TransceiverElement(this.transceiverShare, mail, types.get(seed % types.size()), this.commonKey);
    }

}
