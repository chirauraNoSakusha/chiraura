package nippon.kawauso.chiraura.messenger;

import java.net.InetSocketAddress;
import java.security.Key;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.connection.PortFunctions;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public final class SecondMessageTest extends BytesConvertibleTest<SecondMessage> {

    static private final List<KeyPair> PUBLIC_KEYS;
    static {
        PUBLIC_KEYS = new ArrayList<>();
        for (int i = 0, n = 97; i < n; i++) {
            PUBLIC_KEYS.add(CryptographicKeys.newPublicKeyPair());
        }
    }

    static private final List<Key> COMMON_KEYS;
    static {
        COMMON_KEYS = new ArrayList<>();
        for (int i = 0, n = 997; i < n; i++) {
            COMMON_KEYS.add(CryptographicKeys.newCommonKey());
        }
    }

    @Override
    protected SecondMessage[] getInstances() {
        return new SecondMessage[] {
                new SecondMessage(PUBLIC_KEYS.get(0), COMMON_KEYS.get(0), new byte[] { 1, 2, 3, 4, 5 }, 3, 1111, 121, new InetSocketAddress(111)),
                new SecondMessage(PUBLIC_KEYS.get(2 % PUBLIC_KEYS.size()), COMMON_KEYS.get(1 % COMMON_KEYS.size()), new byte[] { 6, 7 }, 1, 32435,
                        ConnectionTypes.DEFAULT, new InetSocketAddress(1111)),
        };
    }

    @Override
    protected SecondMessage getInstance(final int seed) {
        return new SecondMessage(PUBLIC_KEYS.get(seed % PUBLIC_KEYS.size()), COMMON_KEYS.get(1 % COMMON_KEYS.size()), new byte[] { (byte) seed,
                (byte) (seed + 1), (byte) (seed + 2) }, seed, seed % (PortFunctions.MAX_VALUE + 1), seed, new InetSocketAddress(seed % (1 << 16)));
    }

    @Override
    protected BytesConvertible.Parser<SecondMessage> getParser() {
        return SecondMessage.getParser();
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
        final KeyPair id = PUBLIC_KEYS.get(0);
        final Key key = COMMON_KEYS.get(0);
        final byte[] watchword = new byte[CryptographicKeys.PUBLIC_KEY_SIZE / Byte.SIZE / 2];

        final SecondMessage instance = new SecondMessage(id, key, watchword, 0, 0, 0, new InetSocketAddress(1));
        Assert.assertArrayEquals(key.getEncoded(), CryptographicFunctions.decrypt(instance.getId(), instance.getEncryptedKey()));
    }

}
