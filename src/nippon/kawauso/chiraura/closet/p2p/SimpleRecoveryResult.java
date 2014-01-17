/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

/**
 * データ片の取り寄せの結果。
 * 1. 成功。取り寄せがちゃんとできた。
 * 2. 失敗。取り寄せがちゃんとできなかった。
 * 3. 不在。取り寄せるデータ片が無かった。
 * 4. 諦め。
 * @author chirauraNoSakusha
 */
final class SimpleRecoveryResult {

    private final boolean giveUp;
    private final boolean notFound;

    private final boolean success;

    private SimpleRecoveryResult(final boolean giveUp, final boolean notFound, final boolean success) {
        this.giveUp = giveUp;
        this.notFound = notFound;
        this.success = success;
    }

    static SimpleRecoveryResult newGiveUp() {
        return new SimpleRecoveryResult(true, false, false);
    }

    static SimpleRecoveryResult newNotFound() {
        return new SimpleRecoveryResult(false, true, false);
    }

    static SimpleRecoveryResult newFailure() {
        return new SimpleRecoveryResult(false, false, false);
    }

    SimpleRecoveryResult() {
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
