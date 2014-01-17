/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

/**
 * データ片の取り寄せの結果。
 * 1. 成功。同期がちゃんとできた。
 * 2. 失敗。同期がちゃんとできなかった。
 * 3. 不在。同期するデータ片が無かった。
 * 4. 諦め。
 * @author chirauraNoSakusha
 */
final class RecoveryResult {

    private final boolean giveUp;
    private final boolean notFound;

    private final boolean success;

    private RecoveryResult(final boolean giveUp, final boolean notFound, final boolean success) {
        this.giveUp = giveUp;
        this.notFound = notFound;
        this.success = success;
    }

    static RecoveryResult newGiveUp() {
        return new RecoveryResult(true, false, false);
    }

    static RecoveryResult newNotFound() {
        return new RecoveryResult(false, true, false);
    }

    static RecoveryResult newFailure() {
        return new RecoveryResult(false, false, false);
    }

    RecoveryResult() {
        this(false, false, true);
    }

    boolean isGivenUp() {
        return this.giveUp;
    }

    boolean isNotFound() {
        return this.notFound;
    }

    boolean isSuccess() {
        return this.success;
    }

}
