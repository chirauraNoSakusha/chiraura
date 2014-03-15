package nippon.kawauso.chiraura.bbs;

/**
 * @author chirauraNoSakusha
 */
final class PostErrorRequest implements Request {

    private final String title;
    private final String comment;

    PostErrorRequest(final String title, final String comment) {
        if (title == null) {
            throw new IllegalArgumentException("Null title.");
        } else if (comment == null) {
            throw new IllegalArgumentException("Null comment.");
        }
        this.title = title;
        this.comment = comment;
    }

    String getTitle() {
        return this.title;
    }

    String getComment() {
        return this.comment;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.title)
                .append(", ").append(this.comment)
                .append(']').toString();
    }

}
