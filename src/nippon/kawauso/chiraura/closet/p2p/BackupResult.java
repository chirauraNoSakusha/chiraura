/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

/**
 * データ片の複製の結果。
 * 1. 成功。同期がちゃんとできた。
 * 2. 失敗。同期がちゃんとできなかった。
 * 3. 諦め。
 * @author chirauraNoSakusha
 */
final class BackupResult {

    private final boolean giveUp;
    private final boolean success;

    private BackupResult(final boolean giveUp, final boolean success) {
        this.giveUp = giveUp;
        this.success = success;
    }

    static BackupResult newGiveUp() {
        return new BackupResult(true, false);
    }

    static BackupResult newFailure() {
        return new BackupResult(false, false);
    }

    BackupResult() {
        this(false, true);
    }

    boolean isGivenUp() {
        return this.giveUp;
    }

    boolean isSuccess() {
        return this.success;
    }

}
