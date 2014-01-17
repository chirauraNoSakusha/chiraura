/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

/**
 * 2chブラウザからの板 (スレ一覧) 取得リクエスト。
 * @author chirauraNoSakusha
 */
final class GetBoardRequest implements Request {

    private final String board;

    private final Long ifModifiedSince;
    private final String ifNoneMatch;

    GetBoardRequest(final String board, final Long ifModifiedSince, final String ifNoneMatch) {
        if (board == null) {
            throw new IllegalArgumentException("Null board.");
        }
        this.board = board;
        this.ifModifiedSince = ifModifiedSince;
        this.ifNoneMatch = ifNoneMatch;
    }

    String getBoard() {
        return this.board;
    }

    Long getIfModifiedSince() {
        return this.ifModifiedSince;
    }

    String getIfNoneMatch() {
        return this.ifNoneMatch;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.board)
                .append(", ").append(this.ifModifiedSince)
                .append(", ").append(this.ifNoneMatch)
                .append(']').toString();
    }

}
