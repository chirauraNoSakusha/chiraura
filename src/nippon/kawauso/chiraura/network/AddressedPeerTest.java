package nippon.kawauso.chiraura.network;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.AddressTest;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

/**
 * @author chirauraNoSakusha
 */
public final class AddressedPeerTest extends BytesConvertibleTest<AddressedPeer> {

    /**
     * 個体をてきとうに作成する。
     * @param random 乱数生成器
     * @return てきとうに作成した個体
     */
    public static AddressedPeer randomInstance(final Random random) {
        final byte[] buff = new byte[4];
        random.nextBytes(buff);
        try {
            return new AddressedPeer(AddressTest.newRandomInstance(random), new InetSocketAddress(InetAddress.getByAddress(buff), random.nextInt(1 << 16)));
        } catch (final UnknownHostException e) {
            // 来ないはず。
            throw new RuntimeException(e);
        }
    }

    /**
     * 試験用に作成する。
     * @param seed 種
     * @return 試験用インスタンス
     */
    public static AddressedPeer newInstance(final int seed) {
        final byte[] buff = new byte[] { (byte) seed, (byte) (seed + 1), (byte) (seed + 2), (byte) (seed + 3) };
        try {
            return new AddressedPeer(AddressTest.newInstance(seed), new InetSocketAddress(InetAddress.getByAddress(buff), seed % (1 << 16)));
        } catch (final UnknownHostException e) {
            // 来ないはず。
            throw new RuntimeException(e);
        }
    }

    @Override
    public AddressedPeer[] getInstances() {
        final Random random = new Random();
        final List<AddressedPeer> list = new ArrayList<>();
        list.add(new AddressedPeer(AddressTest.newRandomInstance(random), new InetSocketAddress("localhost", 111)));
        list.add(new AddressedPeer(AddressTest.newRandomInstance(random), new InetSocketAddress("localhost", 222)));
        return list.toArray(new AddressedPeer[0]);
    }

    @Override
    public AddressedPeer getInstance(final int seed) {
        return new AddressedPeer(new Address(BigInteger.valueOf(seed).abs(), Integer.SIZE), new InetSocketAddress("localhost", seed % (1 << 16)));
    }

    @Override
    protected BytesConvertible.Parser<AddressedPeer> getParser() {
        return AddressedPeer.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

}
