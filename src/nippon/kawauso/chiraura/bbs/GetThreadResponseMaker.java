/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import nippon.kawauso.chiraura.lib.Duration;

/**
 * 2chブラウザからのスレ取得リクエストへの返答を作る。
 * @author chirauraNoSakusha
 */
final class GetThreadResponseMaker {

    private final ClosetWrapper closet;

    GetThreadResponseMaker(final ClosetWrapper closet) {
        if (closet == null) {
            throw new IllegalArgumentException("Null closet.");
        }

        this.closet = closet;
    }

    Response make(final GetThreadRequest request, final long timeout) throws InterruptedException {
        final long start = System.currentTimeMillis();

        final ThreadChunk thread = this.closet.getThread(request.getBoard(), request.getThread(), timeout);
        if (thread != null) {
            final Long ifModifiedSince = request.getIfModifiedSince();
            final String ifNoneMatch = request.getIfNoneMatch();
            if (ifModifiedSince != null && ifNoneMatch != null) {
                if (thread.getUpdateDate() - thread.getUpdateDate() % Duration.SECOND <= ifModifiedSince
                        && Long.toString(thread.getNetworkTag()).equals(ifNoneMatch)) {
                    return new NotModifiedResponse(getTarget(request));
                }
            } else if (ifModifiedSince != null) {
                if (thread.getUpdateDate() - thread.getUpdateDate() % Duration.SECOND <= ifModifiedSince) {
                    return new NotModifiedResponse(getTarget(request));
                }
            } else if (ifNoneMatch != null) {
                if (Long.toString(thread.getNetworkTag()).equals(ifNoneMatch)) {
                    return new NotModifiedResponse(getTarget(request));
                }
            }

            final Integer rangeHead = request.getRangeHead();
            if (rangeHead != null) {
                final byte[] content = thread.toNetworkString().getBytes(Constants.CONTENT_CHARSET);
                if (content.length <= rangeHead) {
                    return new RangeNotSatisfiableResponse(getTarget(request));
                }
                return new PartialContentResponse(thread, content, rangeHead);
            } else {
                return new ContentResponse(thread);
            }
        } else if (start + timeout <= System.currentTimeMillis()) {
            return new InternalServerErrorResponse("時間切れです。");
        } else {
            return new NotFoundResponse(getTarget(request));
        }
    }

    private static String getTarget(final GetThreadRequest request) {
        return (new StringBuilder("/")).append(request.getBoard()).append('/').append(Long.toString(request.getThread())).toString();
    }

}
