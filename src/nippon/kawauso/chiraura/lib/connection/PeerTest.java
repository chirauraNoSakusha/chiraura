package nippon.kawauso.chiraura.lib.connection;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Random;

/**
 * @author chirauraNoSakusha
 */
public final class PeerTest {

    /**
     * ランダムな個体情報をつくる。
     * @param random 乱数源
     * @return ランダムな個体情報
     */
    public static InetSocketAddress newRandomInstance(final Random random) {
        final byte[] buff = new byte[4];
        random.nextBytes(buff);
        try {
            return new InetSocketAddress(InetAddress.getByAddress(buff), random.nextInt(1 << 16));
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

}
