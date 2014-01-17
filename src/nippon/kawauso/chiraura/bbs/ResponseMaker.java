/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

/**
 * 2chブラウザからのリクエストへの返答を作る。
 * @author chirauraNoSakusha
 */
final class ResponseMaker {

    private final GetBoardResponseMaker getBoardResponseMaker;
    private final GetThreadResponseMaker getThreadResponseMaker;
    private final AddThreadResponseMaker addThreadResponseMaker;
    private final AddCommentResponseMaker addCommentResponseMaker;

    ResponseMaker(final ClosetWrapper closet) {
        this.getBoardResponseMaker = new GetBoardResponseMaker(closet);
        this.getThreadResponseMaker = new GetThreadResponseMaker(closet);
        this.addThreadResponseMaker = new AddThreadResponseMaker(closet);
        this.addCommentResponseMaker = new AddCommentResponseMaker(closet);
    }

    Response make(final Request request, final long timeout) throws InterruptedException {
        if (request instanceof GetBoardRequest) {
            return this.getBoardResponseMaker.make((GetBoardRequest) request, timeout);
        } else if (request instanceof GetThreadRequest) {
            return this.getThreadResponseMaker.make((GetThreadRequest) request, timeout);
        } else if (request instanceof AddThreadRequest) {
            return this.addThreadResponseMaker.make((AddThreadRequest) request, timeout);
        } else if (request instanceof AddCommentRequest) {
            return this.addCommentResponseMaker.make((AddCommentRequest) request, timeout);
        } else if (request instanceof PostErrorRequest) {
            final PostErrorRequest request1 = (PostErrorRequest) request;
            return new PostErrorResponse(request1.getTitle(), request1.getComment());
        } else if (request instanceof BadHttpRequest) {
            return new BadRequestResponse(((BadHttpRequest) request).getComment());
        } else if (request instanceof NotFoundRequest) {
            return new NotFoundResponse(((NotFoundRequest) request).getTarget());
        } else if (request instanceof NotImplementedRequest) {
            return new NotImplementedResponse(((NotImplementedRequest) request).getMethod());
        } else {
            return null;
        }
    }

}
