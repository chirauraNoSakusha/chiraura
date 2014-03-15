package nippon.kawauso.chiraura.lib.container;

/**
 * @author chirauraNoSakusha
 */
public final class BasicInputOrderedMapTest extends InputOrderedMapTest {

    @Override
    <K, V> InputOrderedMap<K, V> getInstance() {
        return new BasicInputOrderedMap<>();
    }

}
