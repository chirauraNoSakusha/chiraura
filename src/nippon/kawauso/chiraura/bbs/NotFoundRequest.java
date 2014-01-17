/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

/**
 * @author chirauraNoSakusha
 */
final class NotFoundRequest implements Request {

    private final String target;

    NotFoundRequest(final String target) {
        if (target == null) {
            throw new IllegalArgumentException("Null target.");
        }
        this.target = target;
    }

    String getTarget() {
        return this.target;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.target)
                .append(']').toString();
    }

}
