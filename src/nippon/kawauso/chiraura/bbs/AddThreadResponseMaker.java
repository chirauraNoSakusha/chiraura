/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

/**
 * 2chブラウザからのスレ作成リクエストへの返答を作る。
 * @author chirauraNoSakusha
 */
final class AddThreadResponseMaker {

    private final ClosetWrapper closet;

    AddThreadResponseMaker(final ClosetWrapper closet) {
        if (closet == null) {
            throw new IllegalArgumentException("Null closet.");
        }

        this.closet = closet;
    }

    Response make(final AddThreadRequest request, final long timeout) throws InterruptedException {
        final long start = System.currentTimeMillis();

        final String title = PostFunctions.wrapTitle(request.getTitle());
        final String author = PostFunctions.wrapAuthor(request.getAuthor(), request.getBoard());
        final String mail = PostFunctions.wrapMail(request.getMail());
        final long authorId = PostFunctions.calculateId(request.getBoard(), start);
        final String message = PostFunctions.wrapMessage(request.getComment());
        boolean result;
        try {
            result = this.closet.addThread(request.getBoard(), title, author, mail, start, authorId, message, timeout);
        } catch (final ContentException e) {
            return new PostErrorResponse(e.getComment(), e.getComment());
        }
        if (result) {
            return new PostTrueResponse("書きこみました。", "スレッドを作成しました。");
        } else {
            /*
             * 失敗原因を調べる。
             */
            if (start + timeout <= System.currentTimeMillis()) {
                return new InternalServerErrorResponse("時間切れです。");
            } else {
                return new InternalServerErrorResponse("ごめんなさい。");
            }
        }
    }

}
