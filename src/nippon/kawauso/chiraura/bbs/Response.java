/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 2chブラウザからのリクエストへの返答。
 * @author chirauraNoSakusha
 */
interface Response {

    void toStream(OutputStream output) throws IOException;

}
