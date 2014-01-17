/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.base.Address;
import nippon.kawauso.chiraura.lib.base.HashValue;
import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

/**
 * @author chirauraNoSakusha
 */
public final class AddressAccessMessageTest extends BytesConvertibleTest<AddressAccessMessage> {

    @Override
    protected AddressAccessMessage[] getInstances() {
        final List<AddressAccessMessage> list = new ArrayList<>();
        list.add(new AddressAccessMessage(new Address(HashValue.calculateFromString("いろは").toBigInteger(), HashValue.SIZE)));
        list.add(new AddressAccessMessage(new Address(HashValue.calculateFromString("にほへとち").toBigInteger(), HashValue.SIZE)));
        list.add(new AddressAccessMessage(new Address(HashValue.calculateFromString("りぬる").toBigInteger(), HashValue.SIZE)));
        return list.toArray(new AddressAccessMessage[0]);
    }

    @Override
    protected AddressAccessMessage getInstance(final int seed) {
        return new AddressAccessMessage(new Address(BigInteger.valueOf(seed).abs(), HashValue.SIZE));
    }

    @Override
    protected BytesConvertible.Parser<AddressAccessMessage> getParser() {
        return AddressAccessMessage.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

}
