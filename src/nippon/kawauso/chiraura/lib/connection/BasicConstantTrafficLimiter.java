package nippon.kawauso.chiraura.lib.connection;

import java.net.InetSocketAddress;

/**
 * @author chirauraNoSakusha
 */
public final class BasicConstantTrafficLimiter extends ConstantLimiter<InetSocketAddress> {

    /**
     * 作成する。
     * @param duration 単位監視期間 (ミリ秒)
     * @param sizeLimit 制限する通信量 (バイト)
     * @param countLimit 制限する通信回数
     * @param penalty 制限量に達したときの追加の待ち時間 (ミリ秒)
     */
    public BasicConstantTrafficLimiter(final long duration, final long sizeLimit, final int countLimit, final long penalty) {
        super(duration, sizeLimit, countLimit, penalty);
    }

}
