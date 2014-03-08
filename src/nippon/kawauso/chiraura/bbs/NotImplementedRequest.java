/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import nippon.kawauso.chiraura.lib.http.Http;

/**
 * 2chブラウザからの許されないリクエスト。
 * @author chirauraNoSakusha
 */
final class NotImplementedRequest implements Request {

    private final Http.Method method;

    NotImplementedRequest(final Http.Method method) {
        if (method == null) {
            throw new IllegalArgumentException("Null method.");
        }
        this.method = method;
    }

    Http.Method getMethod() {
        return this.method;
    }

    @Override
    public String toString() {
        return (new StringBuilder(this.getClass().getSimpleName()))
                .append('[').append(this.method)
                .append(']').toString();
    }

}
