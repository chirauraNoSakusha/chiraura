/**
 * 
 */
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
 * @author chirauraNoSakusha
 */
final class GZippedEncryptedEnvelope extends SkeletalEncryptedEnvelope<GZippedEnvelope> {

    private GZippedEncryptedEnvelope(final SkeletalEncryptedEnvelope.Proxy<GZippedEnvelope> proxy) {
        super(proxy);
    }

    GZippedEncryptedEnvelope(final List<Message> mail, final TypeRegistry<Message> registry, final Key encryptionKey) {
        super(new GZippedEnvelope(mail, registry), encryptionKey);
    }

    static BytesConvertible.Parser<GZippedEncryptedEnvelope> getParser(final TypeRegistry<Message> registry, final Key decryptionKey) {
        return new BytesConvertible.Parser<GZippedEncryptedEnvelope>() {
            @Override
            public int fromStream(final InputStream input, final int maxByteSize, final List<? super GZippedEncryptedEnvelope> output) throws MyRuleException,
                    IOException {
                final List<SkeletalEncryptedEnvelope.Proxy<GZippedEnvelope>> proxy = new ArrayList<>(1);
                final int size = Proxy.getParser(decryptionKey, GZippedEnvelope.getParser(registry)).fromStream(input, maxByteSize, proxy);
                output.add(new GZippedEncryptedEnvelope(proxy.get(0)));
                return size;
            }
        };
    }

}
