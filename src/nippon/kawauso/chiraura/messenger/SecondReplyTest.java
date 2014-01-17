/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class SecondReplyTest extends BytesConvertibleTest<SecondReply> {

    static private final List<KeyPair> KEYS;
    static {
        KEYS = new ArrayList<>();
        for (int i = 0, n = 97; i < n; i++) {
            KEYS.add(CryptographicKeys.newPublicKeyPair());
        }
    }

    @Override
    protected SecondReply[] getInstances() {
        return new SecondReply[] {
                new SecondReply(KEYS.get(0), new byte[] { 1, 2, 3, 4, 5 }, KEYS.get(1 % KEYS.size()).getPublic(), 0, new InetSocketAddress(111)),
                new SecondReply(KEYS.get(2 % KEYS.size()), new byte[] { 6, 7 }, KEYS.get(15 % KEYS.size()).getPublic(), 1, new InetSocketAddress(1246)),
        };
    }

    @Override
    protected SecondReply getInstance(final int seed) {
        return new SecondReply(KEYS.get(seed % KEYS.size()), new byte[] { (byte) seed, (byte) (seed + 1), (byte) (seed + 2) },
                KEYS.get((seed + 1) % KEYS.size()).getPublic(), seed, new InetSocketAddress(seed % (1 << 16)));
    }

    @Override
    protected BytesConvertible.Parser<SecondReply> getParser() {
        return SecondReply.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000;
    }

    /**
     * 署名機能の検査。
     * @throws Exception 異常
     */
    @Test
    public void testSignature() throws Exception {
        final KeyPair pair = KEYS.get(0);
        final byte[] watchword = new byte[CryptographicKeys.PUBLIC_KEY_SIZE / Byte.SIZE / 2];
        final Random random = new Random();
        random.nextBytes(watchword);

        final SecondReply instance = new SecondReply(pair, watchword, pair.getPublic(), 0, new InetSocketAddress(1));
        Assert.assertArrayEquals(watchword, CryptographicFunctions.decrypt(instance.getId(), instance.getEncryptedWatchword()));
    }

}
