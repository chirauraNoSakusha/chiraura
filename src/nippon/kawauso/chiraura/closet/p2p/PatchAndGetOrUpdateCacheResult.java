package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.closet.Mountain;

/**
 * 複製も考えた差分追加とデータ片取得の結果。
 * 1. 成功のデータ片。差分を追加できた。
 * 2. 失敗のデータ片。差分を追加できなかった。
 * 3. 不在。データ片が無かった。
 * 4. 諦め。
 * @author chirauraNoSakusha
 */
final class PatchAndGetOrUpdateCacheResult {

    private final boolean giveUp;
    private final boolean success;

    private final Mountain chunk;
    private final long accessDate;

    private PatchAndGetOrUpdateCacheResult(final boolean giveUp, final boolean success, final Mountain chunk, final long accessDate) {
        this.giveUp = giveUp;
        this.success = success;
        this.chunk = chunk;
        this.accessDate = accessDate;
    }

    static PatchAndGetOrUpdateCacheResult newGiveUp() {
        return new PatchAndGetOrUpdateCacheResult(true, false, null, 0);
    }

    static PatchAndGetOrUpdateCacheResult newNotFound(final long accessDate) {
        return new PatchAndGetOrUpdateCacheResult(false, false, null, accessDate);
    }

    PatchAndGetOrUpdateCacheResult(final boolean success, final Mountain chunk, final long accessDate) {
        this(false, success, chunk, accessDate);
    }

    boolean isGivenUp() {
        return this.giveUp;
    }

    boolean isNotFound() {
        return this.chunk == null;
    }

    boolean isSuccess() {
        return this.success;
    }

    Mountain getChunk() {
        return this.chunk;
    }

    long getAccessDate() {
        return this.accessDate;
    }

}
