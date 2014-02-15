/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

/**
 * @author chirauraNoSakusha
 */
abstract class InvalidTargetRequest implements Request {

    private final String target;

    InvalidTargetRequest(final String target) {
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
