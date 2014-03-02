/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import nippon.kawauso.chiraura.lib.Duration;
import nippon.kawauso.chiraura.lib.connection.PortFunctions;

/**
 * 2chブラウザからのスレ取得リクエストへの返答を作る。
 * @author chirauraNoSakusha
 */
final class GetThreadResponseMaker {

    private final ClosetWrapper closet;
    private final int port;

    GetThreadResponseMaker(final ClosetWrapper closet, final int port) {
        if (closet == null) {
            throw new IllegalArgumentException("Null closet.");
        } else if (!PortFunctions.isValid(port)) {
            throw new IllegalArgumentException("Invalid port ( " + port + " ).");
        }

        this.closet = closet;
        this.port = port;
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

            final byte[] content = thread.toNetworkString(request.getHost(), this.port).getBytes(Constants.CONTENT_CHARSET);
            final Integer rangeHead = request.getRangeHead();
            if (rangeHead != null) {
                if (content.length <= rangeHead) {
                    return new RangeNotSatisfiableResponse(getTarget(request));
                }
                return new PartialContentResponse(thread, content, rangeHead);
            } else {
                return new ContentResponse(thread, content);
            }
        } else if (start + timeout <= System.currentTimeMillis()) {
            return new InternalServerErrorResponse("時間切れです。");
        } else {
            return new NotFoundResponse(getTarget(request));
        }
    }

    private static String getTarget(final GetThreadRequest request) {
        return (new StringBuilder("/")).append(request.getBoard()).append('/').append(request.getThread()).toString();
    }

}
