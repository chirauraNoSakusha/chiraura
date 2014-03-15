package nippon.kawauso.chiraura.closet.p2p;

import nippon.kawauso.chiraura.closet.Mountain;

/**
 * 複製も考えたデータ片取得または更新の結果。
 * 1. データ片。
 * 2. 不在。データ片が無かった。
 * 3. 諦め。
 * @author chirauraNoSakusha
 */
final class GetOrUpdateCacheResult {

    private final boolean giveUp;
    private final Mountain chunk;
    private final long accessDate;

    private GetOrUpdateCacheResult(final boolean giveUp, final Mountain chunk, final long accessDate) {
        this.giveUp = giveUp;
        this.chunk = chunk;
        this.accessDate = accessDate;
    }

    static GetOrUpdateCacheResult newGiveUp() {
        return new GetOrUpdateCacheResult(true, null, 0);
    }

    static GetOrUpdateCacheResult newNotFound(final long accessDate) {
        return new GetOrUpdateCacheResult(false, null, accessDate);
    }

    GetOrUpdateCacheResult(final Mountain chunk, final long accessDate) {
        this(false, chunk, accessDate);
    }

    boolean isGivenUp() {
        return this.giveUp;
    }

    boolean isNotFound() {
        return this.chunk == null;
    }

    Mountain getChunk() {
        return this.chunk;
    }

    long getAccessDate() {
        return this.accessDate;
    }

}
