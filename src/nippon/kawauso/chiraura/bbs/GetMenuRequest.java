/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

/**
 * 2chブラウザからの bbsmenu.html の取得リクエスト。
 * @author chirauraNoSakusha
 */
final class GetMenuRequest implements Request {

    private final String target;
    private final String host;

    GetMenuRequest(final String target, final String host) {
        if (target == null) {
            throw new IllegalArgumentException("Null target.");
        }
        this.target = target;
        this.host = host; // null 可。
    }

    String getTarget() {
        return this.target;
    }

    String getHost() {
        return this.host;
    }

}
