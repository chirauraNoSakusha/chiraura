/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

/**
 * データ片追加の結果。
 * 1. 成功。データ片を追加できた。
 * 2. 失敗。既にデータ片があった。
 * 3. 諦め。
 * @author chirauraNoSakusha
 */
final class AddChunkResult {

    private final boolean givenUp;
    private final boolean success;

    private AddChunkResult(final boolean givenUp, final boolean success) {
        this.givenUp = givenUp;
        this.success = success;
    }

    static AddChunkResult newGiveUp() {
        return new AddChunkResult(true, false);
    }

    static AddChunkResult newFailure() {
        return new AddChunkResult(false, false);
    }

    AddChunkResult() {
        this(false, true);
    }

    boolean isGivenUp() {
        return this.givenUp;
    }

    boolean isSuccess() {
        return this.success;
    }

}
