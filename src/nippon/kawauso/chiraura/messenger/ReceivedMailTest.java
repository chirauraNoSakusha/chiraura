package nippon.kawauso.chiraura.messenger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author chirauraNoSakusha
 */
public final class ReceivedMailTest {

    private static InetSocketAddress randomInetSocketAddress(final Random random) {
        final byte[] buff = new byte[4];
        random.nextBytes(buff);
        try {
            return new InetSocketAddress(InetAddress.getByAddress(buff), random.nextInt(1 << 16));
        } catch (final UnknownHostException e) {
            // 来ないはず。
            throw new RuntimeException(e);
        }
    }

    /**
     * テスト用の試験体を作成する。
     * @param random 作成に使用する乱数生成器
     * @return テスト用の試験体
     */
    public static ReceivedMail newRandomInstance(final Random random) {
        final PublicKey sourceId = CryptographicKeys.newPublicKeyPair().getPublic();
        final InetSocketAddress sourcePeer = randomInetSocketAddress(random);
        final int connectionType = random.nextInt();
        final List<Message> mail = new ArrayList<>();
        mail.add(new TestMessage(random.nextInt()));
        return new BasicReceivedMail(sourceId, sourcePeer, connectionType, mail);
    }
}
