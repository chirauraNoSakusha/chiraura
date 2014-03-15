package nippon.kawauso.chiraura.messenger;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.TypeRegistry;
import nippon.kawauso.chiraura.lib.exception.MyRuleException;

/**
 * 与えられた鍵と即席の乱数で暗号化する封筒。
 * @author chirauraNoSakusha
 */
final class EncryptedWithRandomEnvelope extends SkeletalEncryptedWithRandomEnvelope<PlainEnvelope> {

    private EncryptedWithRandomEnvelope(final SkeletalEncryptedWithRandomEnvelope.Proxy<PlainEnvelope> proxy) {
        super(proxy);
    }

    EncryptedWithRandomEnvelope(final List<Message> mail, final TypeRegistry<Message> registry, final Key encryptionKey) {
        super(new PlainEnvelope(mail, registry), encryptionKey);
    }

    static BytesConvertible.Parser<EncryptedWithRandomEnvelope> getParser(final TypeRegistry<Message> registry, final Key decryptionKey) {
        return new BytesConvertible.Parser<EncryptedWithRandomEnvelope>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super EncryptedWithRandomEnvelope> output)
                    throws MyRuleException, IOException {
                final List<SkeletalEncryptedWithRandomEnvelope.Proxy<PlainEnvelope>> proxy = new ArrayList<>(1);
                final int size = SkeletalEncryptedWithRandomEnvelope.Proxy.getParser(decryptionKey, PlainEnvelope.getParser(registry))
                        .fromStream(input, maxByteSize, proxy);
                output.add(new EncryptedWithRandomEnvelope(proxy.get(0)));
                return size;
            }
        };
    }

}
