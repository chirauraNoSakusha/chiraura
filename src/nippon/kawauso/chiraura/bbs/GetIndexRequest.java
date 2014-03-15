package nippon.kawauso.chiraura.bbs;

/**
 * 2chブラウザからの板直下の取得リクエスト。
 * @author chirauraNoSakusha
 */
final class GetIndexRequest implements Request {

    private final String board;

    GetIndexRequest(final String board) {
        if (board == null) {
            throw new IllegalArgumentException("Null board.");
        }
        this.board = board;
    }

    String getBoard() {
        return this.board;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.board)
                .append(']').toString();
    }

}
