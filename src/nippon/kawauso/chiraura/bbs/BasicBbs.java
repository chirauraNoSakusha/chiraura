/**
 * 
 */
package nippon.kawauso.chiraura.bbs;

import java.util.concurrent.ExecutorService;

import nippon.kawauso.chiraura.closet.Closet;
import nippon.kawauso.chiraura.lib.connection.PortFunctions;

/**
 * @author chirauraNoSakusha
 */
public final class BasicBbs implements Bbs {

    private final int port;
    private final long connectionTimeout;
    private final long internalTimeout;
    private final ClosetWrapper closet;

    /**
     * 作成する。
     * @param port 受け付けポート番号
     * @param connectionTimeout クライアントからの通信を待つ時間 (ミリ秒)
     * @param internalTimeout 内部動作を待つ時間 (ミリ秒)
     * @param closet 四次元押し入れ
     * @param updateThreshold 板更新自粛期間
     */
    public BasicBbs(final int port, final long connectionTimeout, final long internalTimeout, final Closet closet, final long updateThreshold) {
        if (!PortFunctions.isValid(port)) {
            throw new IllegalArgumentException("Invalid port ( " + port + " ).");
        } else if (connectionTimeout < 0) {
            throw new IllegalArgumentException("Negative connection timeout ( " + connectionTimeout + " ).");
        } else if (internalTimeout < 0) {
            throw new IllegalArgumentException("Negative internal timeout ( " + internalTimeout + " ).");
        } else if (updateThreshold < 0) {
            throw new IllegalArgumentException("Negative update threshold ( " + updateThreshold + " ).");
        } else if (closet == null) {
            throw new IllegalArgumentException("Null closet.");
        }

        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.internalTimeout = internalTimeout;
        this.closet = new ClosetWrapper(closet, updateThreshold);
    }

    @Override
    public void start(final ExecutorService executor) {
        executor.submit(new Boss(this.port, this.connectionTimeout, this.internalTimeout, this.closet, executor));
    }

    @Override
    public void close() {}

}
