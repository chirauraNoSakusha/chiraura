/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

/**
 * 2chブラウザからのスレ取得リクエスト。
 * @author chirauraNoSakusha
 */
final class GetThreadRequest implements Request {

    private final String board;
    private final long thread;

    private final Long ifModifiedSince;
    private final String ifNoneMatch;
    private final Integer rangeHead;

    private final String host;

    GetThreadRequest(final String board, final long thread, final Long ifModifiedSince, final String ifNoneMatch, final Integer rangeHead, final String host) {
        if (board == null) {
            throw new IllegalArgumentException("Null board.");
        }
        this.board = board;
        this.thread = thread;
        this.ifModifiedSince = ifModifiedSince;
        this.ifNoneMatch = ifNoneMatch;
        this.rangeHead = rangeHead;
        this.host = host; // null 可。
    }

    String getBoard() {
        return this.board;
    }

    long getThread() {
        return this.thread;
    }

    Long getIfModifiedSince() {
        return this.ifModifiedSince;
    }

    String getIfNoneMatch() {
        return this.ifNoneMatch;
    }

    Integer getRangeHead() {
        return this.rangeHead;
    }

    String getHost() {
        return this.host;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.board)
                .append(", ").append(Long.toString(this.thread))
                .append(", ").append(this.ifModifiedSince)
                .append(", ").append(this.ifNoneMatch)
                .append(", ").append(this.rangeHead)
                .append(']').toString();
    }

}
