/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

/**
 * @author chirauraNoSakusha
 */
public final class FirstMessageTest extends BytesConvertibleTest<FirstMessage> {

    static private final List<PublicKey> KEYS;
    static {
        KEYS = new ArrayList<>();
        for (int i = 0, n = 100; i < n; i++) {
            KEYS.add(CryptographicKeys.newPublicKeyPair().getPublic());
        }
    }

    @Override
    protected FirstMessage[] getInstances() {
        final List<FirstMessage> list = new ArrayList<>();
        list.add(new FirstMessage(KEYS.get(0)));
        list.add(new FirstMessage(KEYS.get(2 % KEYS.size())));
        return list.toArray(new FirstMessage[0]);
    }

    @Override
    protected FirstMessage getInstance(final int seed) {
        return new FirstMessage(KEYS.get(seed % KEYS.size()));
    }

    @Override
    protected BytesConvertible.Parser<FirstMessage> getParser() {
        return FirstMessage.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

}
