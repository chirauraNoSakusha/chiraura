package nippon.kawauso.chiraura.lib.connection;

/**
 * 量・回数を制限する。
 * checkPenalty の返り値 が 0 の場合だけ次の行動をして良い。
 * @author chirauraNoSakusha
 * @param <T> 識別子の型
 */
public interface Limiter<T> {

    /**
     * 量を報告して必要な自粛時間を得る。
     * @param key 識別子
     * @param value 量
     * @return 自粛期間 (ミリ秒)。自粛しなくて良い場合は 0
     * @throws InterruptedException 割り込まれた場合
     */
    public long addValueAndCheckPenalty(T key, long value) throws InterruptedException;

    /**
     * 必要な自粛時間を得る。
     * @param key 識別子
     * @return 自粛期間 (ミリ秒)。自粛しなくて良い場合は 0
     * @throws InterruptedException 割り込まれた場合
     */
    public long checkPenalty(T key) throws InterruptedException;

    /**
     * 必要なければ、key に関するリソースを解放する。
     * @param key 識別子
     * @throws InterruptedException 割り込まれた場合
     */
    public void remove(T key) throws InterruptedException;

}
