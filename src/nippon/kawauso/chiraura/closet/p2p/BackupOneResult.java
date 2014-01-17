/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

/**
 * データ片の需要確認と複製の結果。
 * 1. 成功。複製がちゃんとできた。
 * 2. 失敗。複製がちゃんとできなかった。
 * 3. 諦め。
 * @author chirauraNoSakusha
 */
final class BackupOneResult {

    private final boolean giveUp;
    private final boolean success;

    private BackupOneResult(final boolean giveUp, final boolean success) {
        this.giveUp = giveUp;
        this.success = success;
    }

    static BackupOneResult newGiveUp() {
        return new BackupOneResult(true, false);
    }

    static BackupOneResult newFailure() {
        return new BackupOneResult(false, false);
    }

    BackupOneResult() {
        this(false, true);
    }

    boolean isGivenUp() {
        return this.giveUp;
    }

    boolean isSuccress() {
        return this.success;
    }

}
