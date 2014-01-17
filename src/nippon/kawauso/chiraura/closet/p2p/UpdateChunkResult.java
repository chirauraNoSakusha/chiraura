/**
 * 
 */
package nippon.kawauso.chiraura.closet.p2p;

import java.util.List;

import nippon.kawauso.chiraura.closet.Mountain;

/**
 * データ片の更新の結果。
 * 1. 更新差分。
 * 2. データ片が無かった。
 * 3. 諦めた。
 * @author chirauraNoSakusha
 */
final class UpdateChunkResult {

    private final boolean givenUp;

    final List<? extends Mountain.Dust<?>> diffs;

    private UpdateChunkResult(final boolean givenUp, final List<? extends Mountain.Dust<?>> diffs) {
        this.givenUp = givenUp;
        this.diffs = diffs;
    }

    static UpdateChunkResult newGiveUp() {
        return new UpdateChunkResult(true, null);
    }

    static UpdateChunkResult newNotFound() {
        return new UpdateChunkResult(false, null);
    }

    UpdateChunkResult(final List<? extends Mountain.Dust<?>> diffs) {
        this(false, diffs);
        if (diffs == null) {
            throw new IllegalArgumentException("Null diffs.");
        }
    }

    boolean isGivenUp() {
        return this.givenUp;
    }

    boolean isNotFound() {
        return this.diffs == null;
    }

    List<? extends Mountain.Dust<?>> getDiffs() {
        return this.diffs;
    }

}
