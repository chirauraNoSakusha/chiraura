/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import nippon.kawauso.chiraura.lib.Duration;

/**
 * 2chブラウザからの板 (スレ一覧) 取得リクエストへの返答を作る。
 * @author chirauraNoSakusha
 */
final class GetBoardResponseMaker {

    private final ClosetWrapper closet;

    GetBoardResponseMaker(final ClosetWrapper closet) {
        if (closet == null) {
            throw new IllegalArgumentException("Null closet.");
        }

        this.closet = closet;
    }

    Response make(final GetBoardRequest request, final long timeout) throws InterruptedException {
        final long start = System.currentTimeMillis();

        final BoardChunk board = this.closet.getBoard(request.getBoard(), timeout);
        if (board != null) {
            final Long ifModifiedSince = request.getIfModifiedSince();
            final String ifNoneMatch = request.getIfNoneMatch();
            if (ifModifiedSince != null && ifNoneMatch != null) {
                if (board.getUpdateDate() - board.getUpdateDate() % Duration.SECOND <= ifModifiedSince
                        && Long.toString(board.getNetworkTag()).equals(ifNoneMatch)) {
                    return new NotModifiedResponse(getTarget(request));
                }
            } else if (ifModifiedSince != null) {
                if (board.getUpdateDate() - board.getUpdateDate() % Duration.SECOND <= ifModifiedSince) {
                    return new NotModifiedResponse(getTarget(request));
                }
            } else if (ifNoneMatch != null) {
                if (Long.toString(board.getNetworkTag()).equals(ifNoneMatch)) {
                    return new NotModifiedResponse(getTarget(request));
                }
            }

            return new BoardResponse(board);
        } else if (start + timeout <= System.currentTimeMillis()) {
            return new InternalServerErrorResponse("時間切れです。");
        } else {
            return new InternalServerErrorResponse("ごめんなさい。");
        }
    }

    private static String getTarget(final GetBoardRequest request) {
        return (new StringBuilder("/")).append(request.getBoard()).toString();
    }
}
