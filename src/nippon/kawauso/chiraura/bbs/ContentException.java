/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

@SuppressWarnings("serial")
final class ContentException extends RuntimeException {
    private final String title;
    private final String comment;

    ContentException(final String title, final String comment) {
        super();
        if (title == null) {
            throw new IllegalArgumentException("Null title.");
        } else if (comment == null) {
            throw new IllegalArgumentException("Null comment.");
        }
        this.title = title;
        this.comment = comment;
    }

    ContentException(final String title) {
        this(title, title);
    }

    String getTitle() {
        return this.title;
    }

    String getComment() {
        return this.comment;
    }

    @Override
    public String toString() {
        return super.toString() + " [" + this.title + "]";
    }

}
