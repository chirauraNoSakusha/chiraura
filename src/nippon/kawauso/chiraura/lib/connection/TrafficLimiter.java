/**
 * 
 */
package nippon.kawauso.chiraura.lib.connection;

import java.net.InetSocketAddress;

/**
 * 通信量・通信回数を制限するため。
 * nextSleep が 0 の場合だけ通信して良い。
 * @author chirauraNoSakusha
 */
public interface TrafficLimiter {

    /**
     * 通信サイズを報告して必要な通信自粛時間を得る。
     * @param size 通信したサイズ
     * @param destination 通信先
     * @return 自粛期間。自粛期間 (ミリ秒)。自粛しなくて良い場合は 0
     * @throws InterruptedException 割り込まれた場合
     */
    public long nextSleep(long size, InetSocketAddress destination) throws InterruptedException;

    /**
     * 必要な通信自粛時間を得る。
     * @param destination 通信先
     * @return 自粛期間 (ミリ秒)。自粛しなくて良い場合は 0
     * @throws InterruptedException 割り込まれた場合
     */
    public long nextSleep(InetSocketAddress destination) throws InterruptedException;

    /**
     * 必要なければ、destination に関するリソースを解放する。
     * @param destination 通信先
     * @throws InterruptedException 割り込まれた場合
     */
    public void remove(InetSocketAddress destination) throws InterruptedException;

}
