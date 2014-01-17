/**
 * 
 */
package nippon.kawauso.chiraura.lib.container;

/**
 * @author chirauraNoSakusha
 */
final class ComparableLinkedCell<T extends Comparable<T>> extends LinkedCell<T> implements Comparable<ComparableLinkedCell<T>> {

    ComparableLinkedCell(final T value) {
        super(value);
    }

    @Override
    public int compareTo(final ComparableLinkedCell<T> o) {
        return get().compareTo(o.get());
    }

}
