/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.util.concurrent.ExecutorService;

/**
 * @author chirauraNoSakusha
 */
public interface Bbs extends AutoCloseable {

    /**
     * 接続受け付けを始める。
     * @param executor 実行機
     */
    public void start(ExecutorService executor);

    @Override
    public void close();

}
