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
 * 与えられた鍵で暗号化する封筒。
 * @author chirauraNoSakusha
 */
final class EncryptedEnvelope extends SkeletalEncryptedEnvelope<PlainEnvelope> {

    private EncryptedEnvelope(final SkeletalEncryptedEnvelope.Proxy<PlainEnvelope> proxy) {
        super(proxy);
    }

    EncryptedEnvelope(final List<Message> mail, final TypeRegistry<Message> registry, final Key encryptionKey) {
        super(new PlainEnvelope(mail, registry), encryptionKey);
    }

    static BytesConvertible.Parser<EncryptedEnvelope> getParser(final TypeRegistry<Message> registry, final Key decryptionKey) {
        return new BytesConvertible.Parser<EncryptedEnvelope>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super EncryptedEnvelope> output) throws MyRuleException,
                    IOException {
                final List<SkeletalEncryptedEnvelope.Proxy<PlainEnvelope>> proxy = new ArrayList<>(1);
                final int size = Proxy.getParser(decryptionKey, PlainEnvelope.getParser(registry)).fromStream(input, maxByteSize, proxy);
                output.add(new EncryptedEnvelope(proxy.get(0)));
                return size;
            }
        };
    }

}
