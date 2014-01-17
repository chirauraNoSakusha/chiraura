/**
 * 
 */
package nippon.kawauso.chiraura.lib.container;

/**
 * @author chirauraNoSakusha
 */
public final class BasicInputOrderedNavigableMapTest extends InputOrderedMapTest {

    @Override
    <K, V> BasicInputOrderedNavigableMap<K, V> getInstance() {
        return new BasicInputOrderedNavigableMap<>();
    }

}
