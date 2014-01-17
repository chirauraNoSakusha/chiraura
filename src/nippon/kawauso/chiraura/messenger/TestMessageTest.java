/**
 * 
 */
package nippon.kawauso.chiraura.messenger;

import java.util.ArrayList;
import java.util.List;

import nippon.kawauso.chiraura.lib.converter.BytesConvertible;
import nippon.kawauso.chiraura.lib.converter.BytesConvertibleTest;

/**
 * @author chirauraNoSakusha
 */
public final class TestMessageTest extends BytesConvertibleTest<TestMessage> {

    @Override
    protected TestMessage[] getInstances() {
        final List<TestMessage> list = new ArrayList<>();
        list.add(new TestMessage("いろはに"));
        list.add(new TestMessage(124899));
        list.add(new TestMessage(6342, "ほへと"));
        return list.toArray(new TestMessage[0]);
    }

    @Override
    protected TestMessage getInstance(final int seed) {
        return new TestMessage(seed, Integer.toString(seed));
    }

    @Override
    protected BytesConvertible.Parser<TestMessage> getParser() {
        return TestMessage.getParser();
    }

    @Override
    protected int getNumOfLoops() {
        return 1_000_000;
    }

}
