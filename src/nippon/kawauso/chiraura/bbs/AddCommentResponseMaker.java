/**
 * 
 */
package nippon.kawauso.chiraura.bbs;


/**
 * 2chブラウザからの書き込みリクエストへの返答を作る。
 * @author chirauraNoSakusha
 */
final class AddCommentResponseMaker {

    private final ClosetWrapper closet;
    private final Menu menu;

    AddCommentResponseMaker(final ClosetWrapper closet, final Menu menu) {
        if (closet == null) {
            throw new IllegalArgumentException("Null closet.");
        } else if (menu == null) {
            throw new IllegalArgumentException("Null menu.");
        }

        this.closet = closet;
        this.menu = menu;
    }

    Response make(final AddCommentRequest request, final long timeout) throws InterruptedException {
        final long start = System.currentTimeMillis();

        final String author = PostFunctions.wrapAuthor(request.getAuthor(), request.getBoard(), this.menu);
        final String mail = PostFunctions.wrapMail(request.getMail());
        final long authorId = PostFunctions.calculateId(request.getBoard(), start, request.getSource());
        final String message = PostFunctions.wrapMessage(request.getComment());
        boolean result;
        try {
            result = this.closet.addComment(request.getBoard(), request.getThread(), author, mail, start, authorId, message, timeout);
        } catch (final ContentException e) {
            return new PostErrorResponse(e.getComment(), e.getComment());
        }
        if (result) {
            return new PostTrueResponse("書きこみました。", "レスを追加しました。");
        } else {
            /*
             * 書き込みが失敗した場合はその理由を調べる。
             */
            if (start + timeout <= System.currentTimeMillis()) {
                return new InternalServerErrorResponse("時間切れです。");
            }
            final ThreadChunk thread = this.closet.getThread(request.getBoard(), request.getThread(), start + timeout - System.currentTimeMillis());
            if (thread == null) {
                if (start + timeout <= System.currentTimeMillis()) {
                    return new InternalServerErrorResponse("時間切れです。");
                } else {
                    return new InternalServerErrorResponse("ごめんなさい。");
                }
            } else if (thread.isFull()) {
                return new PostErrorResponse("このスレッドには書き込めません。", "もういっぱいです。");
            } else {
                return new InternalServerErrorResponse("ごめんなさい。");
            }
        }
    }

}
