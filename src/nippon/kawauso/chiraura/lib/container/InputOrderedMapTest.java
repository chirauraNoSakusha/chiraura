/**
 * 
 */
package nippon.kawauso.chiraura.lib.container;

import java.util.Map;
import java.util.Random;

import nippon.kawauso.chiraura.lib.test.UniqueRandom;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chirauraNoSakusha
 */
public abstract class InputOrderedMapTest {

    abstract <K, V> InputOrderedMap<K, V> getInstance();

    /**
     * removeEldest() の検査。
     */
    @Test
    public void testRemoveEldest() {
        final int length = 1_000;

        final InputOrderedMap<Integer, Integer> instance = getInstance();

        for (int i = 0; i < length; i++) {
            instance.put(i, -i);
        }

        for (int i = 0; i < length; i++) {
            Assert.assertEquals(length - i, instance.size());
            final Map.Entry<Integer, Integer> entry = instance.removeEldest();
            Assert.assertEquals((Integer) i, entry.getKey());
            Assert.assertEquals((Integer) (-i), entry.getValue());
        }
        Assert.assertTrue(instance.isEmpty());
        Assert.assertNull(instance.removeEldest());
    }

    /**
     * remove() の検査。
     */
    @Test
    public void testRemove() {
        final int length = 1_000;

        final InputOrderedMap<Integer, Integer> instance = getInstance();

        for (int i = 0; i < length; i++) {
            instance.put(i, -i);
        }

        final Random random = new Random();
        final UniqueRandom uniqueRandom = new UniqueRandom(random);
        final int[] keys = uniqueRandom.getInts(0, length, length);
        for (int i = 0; i < length; i++) {
            Assert.assertEquals(length - i, instance.size());
            final Integer value = instance.remove(keys[i]);
            Assert.assertEquals(keys[i], -value);
        }
        Assert.assertTrue(instance.isEmpty());
        Assert.assertNull(instance.removeEldest());
    }

    /**
     * remove() と removeEldest() を混ぜた検査。
     */
    @Test
    public void test1() {
        final int length = 1_000_000;

        final InputOrderedMap<Integer, Integer> instance = getInstance();

        for (int i = 0; i < length; i++) {
            instance.put(i, -i);
        }

        final Random random = new Random();
        final UniqueRandom uniqueRandom = new UniqueRandom(random);
        final int[] keys = uniqueRandom.getInts(0, length, length);
        long sum = 0;
        for (int i = 0; i < length; i++) {
            Assert.assertEquals(length - i, instance.size());
            Integer key;
            Integer value;
            if (random.nextDouble() < 0.5) {
                // remove().
                key = keys[i];
                value = instance.remove(key);
                if (value != null) {
                } else {
                    final Map.Entry<Integer, Integer> entry = instance.removeEldest();
                    key = entry.getKey();
                    value = entry.getValue();
                }
            } else {
                // removeEldest().
                final Map.Entry<Integer, Integer> entry = instance.removeEldest();
                key = entry.getKey();
                value = entry.getValue();
            }
            sum += value;
            Assert.assertEquals((int) key, -value);
        }
        Assert.assertTrue(instance.isEmpty());
        Assert.assertNull(instance.removeEldest());
        Assert.assertEquals(-length * (length - 1L) / 2, sum);
    }

    /**
     * remove() と removeEldest() と 既登録要素の put() を混ぜた検査。
     */
    @Test
    public void test2() {
        final int length = 1_000_000;

        final InputOrderedMap<Integer, Integer> instance = getInstance();

        for (int i = 0; i < length; i++) {
            instance.put(i, -i);
        }

        final Random random = new Random();
        final UniqueRandom uniqueRandom = new UniqueRandom(random);
        final int[] keys = uniqueRandom.getInts(0, length, length);
        long sum = 0;
        for (int i = 0; i < length; i++) {
            Assert.assertEquals(length - i, instance.size());
            Integer key;
            Integer value;
            if (random.nextDouble() < 0.5) {
                if (instance.containsKey(keys[i])) {
                    if (random.nextDouble() < 0.5) {
                        // put()
                        instance.put(keys[i], -keys[i]);
                    }
                    // remove().
                    key = keys[i];
                    value = instance.remove(key);
                } else {
                    final Map.Entry<Integer, Integer> entry = instance.removeEldest();
                    key = entry.getKey();
                    value = entry.getValue();
                }
            } else {
                // removeEldest().
                final Map.Entry<Integer, Integer> entry = instance.removeEldest();
                key = entry.getKey();
                value = entry.getValue();
            }
            sum += value;
            Assert.assertEquals((int) key, -value);
        }
        Assert.assertTrue(instance.isEmpty());
        Assert.assertNull(instance.removeEldest());
        Assert.assertEquals(-length * (length - 1L) / 2, sum);
    }

}
