package nippon.kawauso.chiraura.bbs;

/**
 * 2chブラウザからの変なリクエスト。
 * @author chirauraNoSakusha
 */
final class BadHttpRequest implements Request {

    private final String comment;

    BadHttpRequest(final String comment) {
        if (comment == null) {
            throw new IllegalArgumentException("Null comment.");
        }
        this.comment = comment;
    }

    String getComment() {
        return this.comment;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.comment)
                .append(']').toString();
    }

}
