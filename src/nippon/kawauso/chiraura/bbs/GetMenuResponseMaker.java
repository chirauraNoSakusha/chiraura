package nippon.kawauso.chiraura.bbs;

import nippon.kawauso.chiraura.lib.connection.PortFunctions;

/**
 * 2chブラウザからのスレ作成リクエストへの返答を作る。
 * @author chirauraNoSakusha
 */
final class GetMenuResponseMaker {

    private final Menu menu;
    private final int port;

    GetMenuResponseMaker(final Menu menu, final int port) {
        if (menu == null) {
            throw new IllegalArgumentException("Null menu.");
        } else if (!PortFunctions.isValid(port)) {
            throw new IllegalArgumentException("Invalid port ( " + port + " ).");
        }

        this.menu = menu;
        this.port = port;
    }

    Response make(final GetMenuRequest request) {

        if (request.getHost() == null) {
            return new NotFoundResponse(request.getTarget());
        }

        final byte[] content = this.menu.toNetworkString(request.getHost(), this.port).getBytes(Constants.CONTENT_CHARSET);
        return new ContentResponse(this.menu, content);
    }

}
