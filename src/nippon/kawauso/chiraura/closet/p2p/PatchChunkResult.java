package nippon.kawauso.chiraura.closet.p2p;

/**
 * 差分追加の結果。
 * 1. 成功。差分を追加できた。
 * 2. 失敗。差分を追加できなかった。
 * 3. 不在。データ片が無かった。
 * 4. 諦め。
 * @author chirauraNoSakusha
 */
final class PatchChunkResult {

    private final boolean givenUp;
    private final boolean notFound;
    private final boolean success;

    PatchChunkResult(final boolean givenUp, final boolean notFound, final boolean success) {
        this.givenUp = givenUp;
        this.notFound = notFound;
        this.success = success;
    }

    static PatchChunkResult newGiveUp() {
        return new PatchChunkResult(true, false, false);
    }

    static PatchChunkResult newNotFound() {
        return new PatchChunkResult(false, true, false);
    }

    static PatchChunkResult newFailure() {
        return new PatchChunkResult(false, false, false);
    }

    PatchChunkResult() {
        this(false, false, true);
    }

    boolean isGivenUp() {
        return this.givenUp;
    }

    boolean isNotFound() {
        return this.notFound;
    }

    boolean isSuccess() {
        return this.success;
    }

}
