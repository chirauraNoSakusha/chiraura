/**
 * 
 */
package nippon.kawauso.chiraura.bbs;


/**
 * 2chブラウザからのスレ作成リクエストへの返答を作る。
 * @author chirauraNoSakusha
 */
final class GetIndexResponseMaker {

    private final Menu menu;

    GetIndexResponseMaker(final Menu menu) {
        if (menu == null) {
            throw new IllegalArgumentException("Null menu.");
        }

        this.menu = menu;
    }

    Response make(final GetIndexRequest request) {
        final String label = this.menu.getLabel(request.getBoard());
        if (label == null) {
            return new IndexResponse(request.getBoard());
        } else {
            return new IndexResponse(request.getBoard(), label);
        }
    }

}
