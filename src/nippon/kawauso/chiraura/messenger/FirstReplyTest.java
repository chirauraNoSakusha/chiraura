/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

/**
 * @author chirauraNoSakusha
 */
public final class FirstReplyTest extends BytesConvertibleTest<FirstReply> {

    static private final List<Key> COMMON_KEYS;
    static {
        COMMON_KEYS = new ArrayList<>();
        for (int i = 0, n = 1_000; i < n; i++) {
            COMMON_KEYS.add(CryptographicKeys.newCommonKey());
        }
    }

    @Override
    protected FirstReply[] getInstances() {
        final List<FirstReply> list = new ArrayList<>();
        list.add(new FirstReply(COMMON_KEYS.get(0)));
        list.add(new FirstReply(COMMON_KEYS.get(1 % COMMON_KEYS.size())));
        return list.toArray(new FirstReply[0]);
    }

    @Override
    protected FirstReply getInstance(final int seed) {
        return new FirstReply(COMMON_KEYS.get(seed % COMMON_KEYS.size()));
    }

    @Override
    protected BytesConvertible.Parser<FirstReply> getParser() {
        return FirstReply.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

}
